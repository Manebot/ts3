package io.manebot.plugin.ts3.platform.server;

import com.github.manevolent.ts3j.api.Channel;
import com.github.manevolent.ts3j.api.Client;
import com.github.manevolent.ts3j.api.ClientProperty;
import com.github.manevolent.ts3j.event.*;
import com.github.manevolent.ts3j.identity.Uid;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;

import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;

import io.manebot.chat.BasicTextChatMessage;
import io.manebot.chat.Chat;
import io.manebot.chat.ChatMessage;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.PluginException;
import io.manebot.plugin.audio.Audio;
import io.manebot.plugin.audio.api.AudioConnection;

import io.manebot.plugin.audio.channel.AudioChannel;
import io.manebot.plugin.audio.channel.AudioChannelRegistrant;
import io.manebot.plugin.audio.mixer.Mixer;

import io.manebot.plugin.audio.opus.OpusParameters;
import io.manebot.plugin.audio.player.AudioPlayer;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;

import io.manebot.plugin.ts3.platform.audio.TeamspeakAudioChannel;
import io.manebot.plugin.ts3.platform.audio.TeamspeakMixerSink;
import io.manebot.plugin.ts3.platform.chat.TeamspeakChat;
import io.manebot.plugin.ts3.platform.chat.TeamspeakChatSender;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakChannel;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakClient;
import io.manebot.plugin.ts3.platform.user.TeamspeakPlatformUser;
import io.manebot.tuple.Pair;
import io.manebot.user.User;
import io.manebot.virtual.Virtual;

import java.io.IOException;

import java.net.InetSocketAddress;

import java.util.*;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TeamspeakServerConnection implements AudioChannelRegistrant, TS3Listener {
    private final TeamspeakPlatformConnection platformConnection;
    private final TeamspeakServer server;
    private final Audio audio;
    private final AudioConnection audioConnection;

    private final LocalTeamspeakClientSocket client;

    // Action service (used to move clients)
    private final ExecutorService actionService;

    private AudioChannel channel;
    private Mixer mixer;
    private Sleeper sleeper; // sleeper, used to move self to Lobby
    private TeamspeakMixerSink sink; // Mixer sink

    // Client and channel lists
    private final Map<Integer, TeamspeakChannel> channels = new LinkedHashMap<>();
    private final Map<Integer, TeamspeakClient> clients = new LinkedHashMap<>();

    public TeamspeakServerConnection(TeamspeakPlatformConnection platformConnection,
                                     TeamspeakServer server,
                                     Audio audio,
                                     AudioConnection audioConnection) {
        this.platformConnection = platformConnection;
        this.server = server;
        this.audio = audio;
        this.audioConnection = audioConnection;

        this.client = createClientSocket();
        this.actionService = Executors.newCachedThreadPool(Virtual.getInstance().currentProcess().newThreadFactory());
    }

    public TeamspeakServer getServer() {
        return server;
    }

    public TeamspeakPlatformConnection getPlatformConnection() {
        return platformConnection;
    }

    private LocalTeamspeakClientSocket createClientSocket() {
        LocalTeamspeakClientSocket clientSocket = new LocalTeamspeakClientSocket();

        this.client.setCommandExecutorService(Executors.newSingleThreadExecutor(
                Virtual.getInstance().currentProcess().newThreadFactory()
        ));

        this.client.setExceptionHandler(
                throwable -> Logger.getGlobal().log(Level.WARNING, "Problem in TS3J", throwable)
        );

        this.client.addListener(this);

        this.client.setNickname(server.getDisplayName() == null ? "Bot" : server.getDisplayName());

        return clientSocket;
    }

    public String getId() {
        return server.getId();
    }

    private String getFriendlyName() {
        return server.getId() + "/" + server.getEndpoint();
    }

    public boolean isConnected() {
        return client.getState() == ClientConnectionState.CONNECTED;
    }

    public AudioChannel getAudioChannel() {
        return channel;
    }

    public CompletionStage<TeamspeakServerConnection> connectAsync() {
        CompletableFuture<TeamspeakServerConnection> future = new CompletableFuture<>();

        new Thread(() -> {
            try {
                future.complete(connect());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }).start();

        return future;
    }

    public TeamspeakServerConnection connect() throws Exception {
        Throwable exception = null;

        if (!server.isEnabled()) throw new IllegalStateException("server " + getFriendlyName() + " is not enabled");

        try {
            // Ensure identity is set at startup
            client.setIdentity(platformConnection.getIdentity());

            String[] components = server.getEndpoint().split("\\:", 2);
            if (components.length == 1) // This specific block is needed because of TS3DNS
                client.connect(components[0], server.getPassword(), 10000L);
            else // Traditional DNS
                client.connect(
                        new InetSocketAddress(components[0], Integer.parseInt(components[1])),
                        server.getPassword(),
                        10000L
                );

            client.subscribeAll();

            for (Channel channel : client.listChannels())
                recognizeChannel(channel);

            for (Client basicClient : client.listClients())
                recognizeClient(client.getClientInfo(basicClient.getId()));

            if (recognizeClient(client.getClientInfo(client.getClientId())) == null)
                throw new IllegalStateException("couldn't find self (clientId=" + client.getClientId() +")");

            onConnected();
        } catch (Throwable e) {
            exception = e;

            if (client.isConnected()) {
                try {
                    client.disconnect();
                } catch (Exception e1) {
                    e.addSuppressed(e);
                }
            }

            throw new IOException(
                    "Problem connecting to Teamspeak server (" + getFriendlyName() + ")",
                    exception
            );
        }

        return this;
    }

    private TeamspeakChannel recognizeChannel(Channel channel) {
        TeamspeakChannel teamspeakChannel = findChannelById(channel.getId());

        if (teamspeakChannel == null) { // Register channel in maps
            channels.put(channel.getId(), teamspeakChannel = new TeamspeakChannel(this, channel.getId()));
        } else { // Already recognized
            return teamspeakChannel;
        }

        teamspeakChannel.setName(channel.getName());

        // Add clients to channel
        for (TeamspeakClient client : getClients()) {
            if (client.getChannelId() == teamspeakChannel.getChannelId()) {
                client.setChannel(teamspeakChannel);
                teamspeakChannel.addClient(client);
            }
        }

        return teamspeakChannel;
    }

    private TeamspeakClient recognizeClient(Client client) {
        if (client == null) return null;

        TeamspeakClient teamspeakClient = findClientById(client.getId());

        if (teamspeakClient == null) { // Register client in maps
            clients.put(
                    client.getId(),
                    teamspeakClient = new TeamspeakClient(this, client.getId(), new Uid(client.getUniqueIdentifier()))
            );
        } else { // Already recognized
            return teamspeakClient;
        }

        // Add client to channel
        if (teamspeakClient.getChannelId() != client.getChannelId()) {
            TeamspeakChannel oldChannel = teamspeakClient.getChannel();
            if (oldChannel != null)
                oldChannel.removeClient(teamspeakClient);

            TeamspeakChannel channel = findChannelById(client.getChannelId());
            teamspeakClient.setChannel(channel);
            teamspeakClient.setChannelId(client.getChannelId());

            if (channel != null)
                channel.addClient(teamspeakClient);
        }

        if (client.getNickname() != null && client.getNickname().length() > 0)
            teamspeakClient.setNickname(client.getNickname());

        teamspeakClient.setListening(!client.isOutputMuted());

        return teamspeakClient;
    }

    private void unrecognizeClient(int id) {
        TeamspeakClient teamspeakClient = findClientById(id);

        if (teamspeakClient != null) {
            TeamspeakChannel teamspeakChannel = teamspeakClient.getChannel();
            if (teamspeakChannel != null)
                teamspeakChannel.removeClient(teamspeakClient);

            // In case something else has a reference to this object
            teamspeakClient.setChannel(null);
            teamspeakClient.setChannelId(-1);

            clients.remove(id);
        }
    }

    private void unrecognizeChannel(int id) {
        channels.remove(id);
    }

    private void onConnected() {
        if (audio != null) registerAudio();

        TeamspeakChannel channel = getLobbyChannel();
        if (channel != null) moveClient(getSelf(), channel);
    }

    private void registerAudio() {
        OpusParameters opusParameters;
        try {
            opusParameters = OpusParameters.fromPluginConfiguration(platformConnection.getPlugin());
        } catch (PluginException e) {
            throw new RuntimeException("problem reading Opus parameters", e);
        }

        this.mixer = audio.createMixer(server.getId(), consumer -> {
            consumer.addDefaultFilters();
            consumer.setFormat(48000f, 2);
        });

        mixer.addSink(sink = new TeamspeakMixerSink(
                TeamspeakMixerSink.AUDIO_FORMAT,
                opusParameters,
                mixer.getBufferSize() * (TeamspeakMixerSink.AUDIO_FORMAT.getSampleSizeInBits()/8)
        ));

        audioConnection.registerChannel(channel = new TeamspeakAudioChannel(mixer, this, this));
        client.setMicrophone(sink);
    }

    private void onDisconnected() {
        clients.clear();
        channels.clear();
        if (sleeper != null) sleeper.cancel();

        unregisterAudio();
    }

    private void unregisterAudio() {
        if (channel != null) {
            audioConnection.unregisterChannel(channel);
            channel = null;
        }

        if (mixer != null) {
            mixer.setRunning(false);
            audioConnection.unregisterMixer(mixer);
            mixer = null;
        }
    }

    public CompletionStage<TeamspeakServerConnection> disconnectAsync() {
        CompletableFuture<TeamspeakServerConnection> future = new CompletableFuture<>();

        new Thread(() -> {
            try {
                disconnect();
                future.complete(this);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }).start();

        return future;
    }

    public TeamspeakServerConnection disconnect()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        if (client.isConnected()) client.disconnect();

        return this;
    }

    public PlatformUser getPlatformUser(TeamspeakClient client) {
        return getPlatformConnection().getPlatformUser(client.getUid());
    }

    public User getUser(TeamspeakClient client) {
        return getPlatformUser(client).getAssociatedUser();
    }

    public Collection<Pair<TeamspeakClient, PlatformUser>> getRegisteredClients(Stream<TeamspeakClient> clients) {
        return clients
                .map(client -> new Pair<>(client, getPlatformUser(client)))
                .filter(pair -> pair.getRight() != null)
                .collect(Collectors.toList());
    }

    public Collection<Pair<TeamspeakClient, PlatformUser>> getRegisteredClients(Collection<TeamspeakClient> clients) {
        return getRegisteredClients(clients.stream());
    }

    public Collection<Pair<TeamspeakClient, PlatformUser>> getRegisteredClients() {
        return getRegisteredClients(getClients());
    }

    public Collection<TeamspeakClient> getClients() {
        return Collections.unmodifiableCollection(new ArrayList<>(clients.values()));
    }

    public Collection<TeamspeakChannel> getChannels() {
        return Collections.unmodifiableCollection(new ArrayList<>(channels.values()));
    }

    public TeamspeakChannel findChannelByName(String name) {
        return getChannels()
                .stream()
                .filter(x -> x.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public TeamspeakChannel findChannelById(int id) {
        return channels.get(id);
    }

    public TeamspeakChannel getLobbyChannel()
            throws IllegalArgumentException {
        if (server.getLobbyChannel() == null) return null;
        return findChannelByName(server.getLobbyChannel());
    }

    public TeamspeakChannel getAwayChannel()
            throws IllegalArgumentException {
        if (server.getAwayChannel() == null) return null;
        return findChannelByName(server.getAwayChannel());
    }

    public TeamspeakClient findClient(Uid uid)
            throws IllegalArgumentException {
        return getClients()
                .stream()
                .filter(x -> x.getUid().equals(uid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "client UID not found: " + uid)
                );
    }

    public TeamspeakClient findClientById(int id)
            throws IllegalArgumentException {
        return clients.get(id);
    }

    public TeamspeakClient getSelf() {
        if (client == null) return null;

        return findClientById(client.getClientId());
    }

    public TeamspeakChannel getCurrentChannel() {
        TeamspeakClient self = getSelf();
        if (self == null) return null;

        return self.getChannel();
    }

    public void follow(TeamspeakClient teamspeakClient) throws IOException {
        try {
            TeamspeakChannel other = teamspeakClient.getChannel();
            if (other == null) throw new NullPointerException("other");

            int cid = other.getChannelId();
            TeamspeakChannel currentChannel = getCurrentChannel();

            if (currentChannel == null || currentChannel.getChannelId() != cid) {
                TeamspeakClient self = getSelf();
                TeamspeakChannel oldChannel = self.getChannel();

                client.joinChannel(cid, null);

                if (oldChannel != null)
                    oldChannel.removeClient(self);

                self.setChannelId(cid);
                self.setChannel(findChannelById(cid));

                TeamspeakChannel newChannel = findChannelById(cid);
                if (newChannel != null)
                    newChannel.addClient(self);
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public void moveClient(TeamspeakClient client, TeamspeakChannel newChannel) {
        if (newChannel == null) throw new NullPointerException();

        if (client.getChannel() == null || client.getChannel().getChannelId() != newChannel.getChannelId())
            try {
                this.client.clientMove(client.getClientId(), newChannel.getChannelId(), null);
            } catch (Exception e) {
                platformConnection.getPlugin().getLogger().log(
                        Level.WARNING,
                        "Failed to move client \"" + client.getNickname() +"\" to " +
                                "channel \"" + newChannel.getName() + "\"",
                        e
                );
            }
    }

    private Sleeper schedule() {
        return schedule(server.getIdleTimeout() * 1_000L);
    }

    private Sleeper schedule(long millis) {
        if (sleeper != null && !sleeper.isDone()) sleeper.cancel();

        Virtual.getInstance()
                .create(sleeper = new Sleeper(System.currentTimeMillis() + millis))
                .start();

        return sleeper;
    }

    private Sleeper reschedule() {
        return reschedule(server.getIdleTimeout() * 1_000L);
    }

    private Sleeper reschedule(long millis) {
        if (sleeper != null && !sleeper.isDone()) {
            sleeper.cancel();

            Virtual.getInstance()
                    .create(sleeper = new Sleeper(System.currentTimeMillis() + millis))
                    .start();
        }

        return sleeper;
    }

    public TeamspeakClient findClientByUser(User user) {
        return getRegisteredClients().stream()
                .filter(pair -> pair.getRight().getAssociatedUser().equals(user))
                .map(Pair::getLeft).max(Comparator.comparingLong(TeamspeakClient::getLastActivity))
                .orElse(null);
    }

    @Override
    public void onPlayerStarted(AudioChannel channel, AudioPlayer player) {
        TeamspeakClient client = findClientByUser(player.getOwner());

        try {
            if (client != null) follow(client);
        } catch (IOException e) {
            // Do nothing
        }
    }

    @Override
    public void onPlayerStopped(AudioChannel channel, AudioPlayer player) {

    }

    @Override
    public void onChannelActivated(AudioChannel channel) {

    }

    @Override
    public void onChannelPassivated(AudioChannel channel) {
        // Move to lobby
    }

    public Chat getServerChat() {
        return getPlatformConnection().getChat(TeamspeakChat.getServerChatId(server));
    }

    public Chat getChannelChat(TeamspeakChannel channel) {
        return getPlatformConnection().getChat(TeamspeakChat.getChannelChatId(server, channel));
    }

    public Chat getPrivateChat(Uid uid) {
        return getPlatformConnection().getChat(TeamspeakChat.getPrivateChatId(server, uid));
    }

    public Chat getPrivateChat(TeamspeakClient client) {
        return getPlatformConnection().getChat(TeamspeakChat.getPrivateChatId(server, client));
    }

    //
    //  Teamspeak 3 Listener callbacks
    //

    @Override
    public void onTextMessage(TextMessageEvent event) {
        TeamspeakClient client = findClientById(event.getInvokerId());
        if (client == null) return; // Ignore unknown clients, NPE protection

        client.setLastActivity(System.currentTimeMillis());

        // Ignore self
        if (client.getClientId() == getSelf().getClientId())
            return;

        TeamspeakPlatformUser user = platformConnection.getPlatformUser(client.getUid());

        TeamspeakChat chat;
        switch (event.getTargetMode()) {
            case CHANNEL:
                chat = platformConnection.getChat(TeamspeakChat.getChannelChatId(server, client.getChannelId()));
                break;
            case CLIENT:
                chat = platformConnection.getChat(TeamspeakChat.getPrivateChatId(server, client.getUid()));
                break;
            case SERVER:
                chat = platformConnection.getChat(TeamspeakChat.getServerChatId(server));
                break;
            default:
                throw new UnsupportedOperationException("unknown message target mode: " + event.getTargetMode().name());
        }

        TeamspeakChatSender chatSender = new TeamspeakChatSender(user, chat);
        ChatMessage chatMessage = new BasicTextChatMessage(chatSender, event.getMessage());
        platformConnection.getPlugin().getBot().getChatDispatcher().executeAsync(chatMessage);
    }

    @Override
    public void onClientJoin(ClientJoinEvent clientJoinEvent) {
        try {
            if (client.getState() == ClientConnectionState.CONNECTED) {
                Client client = this.client.getClientInfo(clientJoinEvent.getClientId());
                TeamspeakClient teamspeakClient = recognizeClient(client);

                // TODO: fire Manebot connection event, associate IP address for AdvancedBan (from my old bot)
            }
        } catch (Exception e) {
            platformConnection.getPlugin().getLogger().log(
                    Level.WARNING,
                    "Problem recognizing client " +
                            clientJoinEvent.getClientId() + "/" +
                            clientJoinEvent.getClientNickname(),
                    e
            );
        }
    }

    @Override
    public void onClientLeave(ClientLeaveEvent clientLeaveEvent) {
        TeamspeakClient client = findClientById(clientLeaveEvent.getClientId());

        if (client != null
                && client.getClientId() != getSelf().getClientId()
                && client.getNickname().equalsIgnoreCase(server.getDisplayName())) {
            TeamspeakServerConnection.this.client.setNickname(server.getDisplayName());
        }

        // Un-recognize from server
        unrecognizeClient(clientLeaveEvent.getClientId());

        // Handle audio
        if (client != null) {
            User user = getUser(client);
            if (user != null) {
                if (channel != null) {
                    List<AudioPlayer> players = channel.getPlayers()
                            .stream()
                            .filter(x -> x.getOwner().equals(user))
                            .collect(Collectors.toList());

                    players.forEach(AudioPlayer::stop);
                }
            }
        }
    }

    @Override
    public void onClientMoved(ClientMovedEvent clientMovedEvent) {
        TeamspeakClient teamspeakClient = findClientById(clientMovedEvent.getClientId());
        if (teamspeakClient != null) {
            switch (clientMovedEvent.getReasonId()) {
                case 0:
                    teamspeakClient.setLastActivity(System.currentTimeMillis());
                    break;
            }

            TeamspeakChannel oldChannel = teamspeakClient.getChannel();
            if (oldChannel != null)
                oldChannel.removeClient(teamspeakClient);

            teamspeakClient.setChannelId(clientMovedEvent.getTargetChannelId());

            TeamspeakChannel newChannel = findChannelById(clientMovedEvent.getTargetChannelId());
            teamspeakClient.setChannel(newChannel);
            if (newChannel != null)
                newChannel.addClient(teamspeakClient);

            // Handle the audio part
            User user = getUser(teamspeakClient);
            if (user != null) {
                List<AudioPlayer> players = channel.getPlayers()
                        .stream()
                        .filter(x -> x.getOwner().equals(user))
                        .collect(Collectors.toList());

                if (players.stream().anyMatch(AudioPlayer::isBlocking)) // Follow them
                    try {
                        follow(teamspeakClient);
                    } catch (IOException e) {
                        for (AudioPlayer player : players) player.stop();
                    }
            }
        }
    }

    @Override
    public void onChannelEdit(ChannelEditedEvent channelEditedEvent) {
        TeamspeakChannel channel = recognizeChannel(new Channel(channelEditedEvent.getMap()));
        // TODO update our local copy of channel name, description, etc.
    }

    @Override
    public void onChannelCreate(ChannelCreateEvent channelCreateEvent) {
        recognizeChannel(new Channel(channelCreateEvent.getMap()));
    }

    @Override
    public void onChannelDeleted(ChannelDeletedEvent channelDeletedEvent) {
        unrecognizeChannel(channelDeletedEvent.getChannelId());
    }

    @Override
    public void onChannelList(ChannelListEvent channelListEvent) {
        recognizeChannel(new Channel(channelListEvent.getMap()));
    }

    @Override
    public void onClientChanged(ClientUpdatedEvent clientUpdatedEvent) {
        int clientId = clientUpdatedEvent.getClientId();
        TeamspeakClient client = findClientById(clientId);
        if (client != null) {
            String outputMuted = clientUpdatedEvent.get(ClientProperty.CLIENT_OUTPUT_MUTED);
            if (outputMuted != null) client.setListening(!outputMuted.equals("1"));

            String nickname = clientUpdatedEvent.get(ClientProperty.CLIENT_NICKNAME);
            if (nickname != null && nickname.length() > 0) client.setNickname(nickname);
        }
    }

    @Override
    public void onDisconnected(DisconnectedEvent disconnectedEvent) {
        onDisconnected();
    }

    @Override
    public void onChannelSubscribed(ChannelSubscribedEvent channelSubscribedEvent) {
        if (client.getState() != ClientConnectionState.CONNECTED) return;

        int subscribedChannelId = channelSubscribedEvent.getChannelId();

        try {
            Channel info = client.getChannelInfo(subscribedChannelId);
            if (info == null) return;

            Map<String, String> infoMap = info.getMap();

            infoMap.put("cid", Integer.toString(subscribedChannelId));

            recognizeChannel(new Channel(infoMap));
        } catch (Exception e) {
            platformConnection.getPlugin().getLogger().log(Level.SEVERE,
                    "Problem obtaining channel information for channel " +
                            subscribedChannelId, e
            );
        }
    }

    private class Sleeper implements Runnable {
        private final long sleepTime;
        private final Object lock = new Object();
        private volatile boolean cancel = false;
        private volatile boolean done = false;

        private Sleeper(long sleepTime) {
            this.sleepTime = sleepTime;
        }

        @Override
        public void run() {
            try {
                synchronized (lock) {
                    while (!cancel) {
                        try {
                            lock.wait(Math.max(0, sleepTime - System.currentTimeMillis()));
                            break;
                        } catch (InterruptedException e) {
                            Thread.yield();
                        }
                    }

                    if (channel != null && !cancel && channel.getState() == AudioChannel.State.WAITING) {
                        TeamspeakChannel lobbyChannel = getLobbyChannel();
                        if (lobbyChannel != null)
                            moveClient(getSelf(), getLobbyChannel());

                        channel.setIdle(true);
                    }

                    done = true;
                }
            } catch (Throwable e) {
                platformConnection.getPlugin().getLogger().log(
                        Level.WARNING,
                        "Problem handling Teamspeak audio channel sleep",
                        e
                );
            }
        }

        void cancel() {
            synchronized (lock) {
                cancel = true;
                lock.notifyAll();
            }
        }

        boolean isDone() {
            return done;
        }
    }
}
