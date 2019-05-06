package io.manebot.plugin.ts3.platform;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.identity.Uid;

import io.manebot.chat.Chat;
import io.manebot.lambda.ThrowingRunnable;
import io.manebot.platform.AbstractPlatformConnection;
import io.manebot.platform.Platform;
import io.manebot.plugin.Plugin;
import io.manebot.plugin.PluginException;
import io.manebot.plugin.audio.Audio;
import io.manebot.plugin.audio.api.AbstractAudioConnection;
import io.manebot.plugin.audio.api.AudioConnection;
import io.manebot.plugin.audio.channel.AudioChannel;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.chat.TeamspeakChannelChat;
import io.manebot.plugin.ts3.platform.chat.TeamspeakChat;
import io.manebot.plugin.ts3.platform.chat.TeamspeakPrivateChat;
import io.manebot.plugin.ts3.platform.chat.TeamspeakServerChat;
import io.manebot.plugin.ts3.platform.server.ServerManager;
import io.manebot.plugin.ts3.platform.server.TeamspeakServerConnection;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakClient;
import io.manebot.plugin.ts3.platform.user.TeamspeakPlatformUser;
import io.manebot.security.ElevationDispatcher;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TeamspeakPlatformConnection extends AbstractPlatformConnection {
    private final Plugin plugin;
    private final Platform platform;
    private final Audio audio;
    private final ElevationDispatcher elevationDispatcher;
    private final TeamspeakAudioConnection audioConnection;
    private final ServerManager serverManager;

    private final List<TeamspeakServerConnection> connections = new LinkedList<>();
    private final Object identityLock = new Object();

    public TeamspeakPlatformConnection(Platform platform, Plugin plugin, Audio audio, ElevationDispatcher elevation) {
        this.platform = platform;
        this.plugin = plugin;
        this.audio = audio;
        this.elevationDispatcher = elevation;
        this.audioConnection = new TeamspeakAudioConnection(audio);
        this.serverManager = plugin.getInstance(ServerManager.class);
    }

    protected TeamspeakPlatformUser loadUserById(Uid uid) {
        return new TeamspeakPlatformUser(this, uid);
    }

    @Override
    protected TeamspeakPlatformUser loadUserById(String id) {
        return loadUserById(new Uid(id));
    }

    public TeamspeakPlatformUser getPlatformUser(Uid uid) {
        return (TeamspeakPlatformUser) super.getCachedUserById(uid.toBase64(), (key) -> loadUserById(uid));
    }

    @Override
    public TeamspeakPlatformUser getPlatformUser(String uid) {
        return (TeamspeakPlatformUser) super.getPlatformUser(uid);
    }

    @Override
    protected TeamspeakChat loadChatById(String id) {
        String[] parts = id.split("\\:");
        String serverId = parts[0].toLowerCase();

        TeamspeakServer server = serverManager.getServer(serverId);
        if (server == null) throw new IllegalArgumentException("unknown Teamspeak3 server: " + serverId);

        TeamspeakChat chat;
        switch (parts[1].toLowerCase()) {
            case "channel":
                if (parts.length != 3) throw new IllegalArgumentException("invalid chat ID: " + id);
                chat = new TeamspeakChannelChat(this, server, Integer.parseInt(parts[2]));
                break;
            case "global":
                if (parts.length != 2) throw new IllegalArgumentException("invalid chat ID: " + id);
                chat = new TeamspeakServerChat(this, server);
                break;
            case "private":
                if (parts.length != 3) throw new IllegalArgumentException("invalid chat ID: " + id);
                chat = new TeamspeakPrivateChat(this, server, new Uid(parts[2]));
                break;
            default:
                throw new UnsupportedOperationException("unsupported Teamspeak3 chat type: " + parts[1]);
        }

        return chat;
    }

    @Override
    public TeamspeakChat getChat(String id) {
        return (TeamspeakChat) super.getChat(id);
    }

    /**
     * Gets the current Teamspeak3 identity set for this platform.
     * @return LocalIdentity instance.
     * @throws IllegalArgumentException
     */
    public LocalIdentity getIdentity() throws IllegalArgumentException {
        synchronized (identityLock) {
            String identityString = plugin.getProperty("identity");

            if (identityString == null) {
                plugin.getLogger().warning("Bot Teamspeak3 identity missing; generating new bot identity...");
                LocalIdentity generatedIdentity;

                try {
                    generatedIdentity = LocalIdentity.generateNew(0);
                    identityString = Base64.getEncoder().encodeToString(
                            generatedIdentity.getPrivateKey().toByteArray()
                    );
                } catch (GeneralSecurityException e) {
                    throw new IllegalArgumentException(e);
                }

                String finalIdentityString = identityString;
                try {
                    elevationDispatcher.elevate(
                            () -> plugin.getRegistration().setProperty("identity", finalIdentityString)
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Problem setting plugin identity property", e);
                }

                plugin.getLogger().info("New bot identity generated: " + generatedIdentity.getUid().toBase64());
            }

            byte[] identityBytes = Base64.getDecoder().decode(identityString);
            LocalIdentity localIdentity = LocalIdentity.load(new BigInteger(identityBytes));

            String keyOffsetString = plugin.getProperty("keyOffset");
            long keyOffset = 0L;
            if (keyOffsetString != null) {
                 keyOffset = Long.parseLong(keyOffsetString);
            }

            // prevent multiple threads from fighting and causing unnecessary CPU
            int securityLevel = Integer.parseInt(plugin.getProperty("securityLevel", "10"));
            if (securityLevel > localIdentity.getSecurityLevel()) {
                plugin.getLogger().warning(
                        "Bot identity security level < " + securityLevel + "; " +
                                "improving security level for bot identity..."
                );

                localIdentity.improveSecurity(securityLevel);
                keyOffset = localIdentity.getKeyOffset();

                long finalKeyOffset = keyOffset;
                try {
                    elevationDispatcher.elevate(
                            () -> plugin.getRegistration().setProperty("keyOffset", Long.toString(finalKeyOffset))
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Problem setting plugin keyOffset property", e);
                }
            }

            localIdentity.setKeyOffset(keyOffset);
            localIdentity.setLastCheckedKeyOffset(keyOffset);

            return localIdentity;
        }
    }

    public AudioConnection getAudioConnection() {
        return audioConnection;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    public TeamspeakServerConnection connectToServer(TeamspeakServer server) {
        TeamspeakServerConnection serverConnection = new TeamspeakServerConnection(
                this,
                server,
                audio,
                audioConnection
        );
        connections.add(serverConnection);
        server.setConnection(serverConnection);
        server.connect();
        return serverConnection;
    }

    @Override
    public void connect() throws PluginException {
        for (TeamspeakServer server : serverManager.getServers()) {
            if (!server.isConnected()) connectToServer(server);
        }
    }

    @Override
    public void disconnect() {
        for (TeamspeakServer server : serverManager.getServers()) {
            if (server.isConnected())
                server.getConnection()
                        .disconnectAsync()
                        .whenComplete((connection, throwable) -> connections.remove(connection));
        }
    }

    @Override
    public TeamspeakPlatformUser getSelf() throws IllegalArgumentException {
        Uid clientUid = getIdentity().getUid();
        return clientUid != null ? getPlatformUser(clientUid) : null;
    }

    @Override
    public Collection<String> getPlatformUserIds() {
        return connections.stream()
                .filter(TeamspeakServerConnection::isConnected)
                .flatMap(connection -> connection.getClients().stream())
                .map(client -> client.getUid().toBase64())
                .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getChatIds() {
        return connections.stream()
                .filter(TeamspeakServerConnection::isConnected)
                .flatMap(connection -> connection.getChannels().stream())
                .map(channel -> TeamspeakChannelChat.getChannelChatId(channel.getConnection().getServer(), channel))
                .collect(Collectors.toList());
    }

    public Stream<TeamspeakClient> findClients(Uid uid) {
        return connections.stream()
                .filter(TeamspeakServerConnection::isConnected)
                .flatMap(connection -> connection.getClients().stream())
                .filter(client -> client.getUid().equals(uid));
    }

    private class TeamspeakAudioConnection extends AbstractAudioConnection {
        public TeamspeakAudioConnection(Audio audio) {
            super(audio);
        }

        @Override
        public AudioChannel getChannel(Chat chat) {
            if (chat instanceof TeamspeakChannelChat) {
                TeamspeakServer server = ((TeamspeakChannelChat) chat).getServer();
                if (!server.isEnabled())
                    return null;

                TeamspeakServerConnection connection = server.getConnection();
                if (connection == null || !connection.isConnected())
                    return null;

                return connection.getAudioChannel();
            } else return null;
        }

        @Override
        public boolean isConnected() {
            return super.isConnected();
        }
    }
}
