package io.manebot.plugin.ts3.platform;

import io.manebot.chat.Chat;
import io.manebot.platform.AbstractPlatformConnection;
import io.manebot.platform.Platform;
import io.manebot.platform.PlatformConnection;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.Plugin;
import io.manebot.plugin.PluginException;
import io.manebot.plugin.audio.Audio;
import io.manebot.plugin.audio.api.AbstractAudioConnection;
import io.manebot.plugin.audio.api.AudioConnection;
import io.manebot.plugin.audio.channel.AudioChannel;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.server.ServerManager;
import io.manebot.plugin.ts3.platform.server.TeamspeakServerConnection;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TeamspeakPlatformConnection extends AbstractPlatformConnection {
    private final Plugin plugin;
    private final Platform platform;
    private final Audio audio;

    private final AudioConnection audioConnection;
    private final ServerManager serverManager;

    private final Map<String, TeamspeakServerConnection> serverConnections = new LinkedHashMap<>();

    public TeamspeakPlatformConnection(Platform platform,
                                       Audio audio) {
        this.audioConnection = new TeamspeakAudioConnection(audio);


        this.platform = platform;
        this.plugin = platform.getPlugin();
        this.audio = audio;

        this.serverManager = plugin.getInstance(ServerManager.class);
    }

    @Override
    protected PlatformUser loadUserById(String id) {
        return null;
    }

    @Override
    protected Chat loadChatById(String id) {
        return null;
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

    @Override
    public void connect() throws PluginException {
        Collection<TeamspeakServer> servers = serverManager.getServers();
        for (TeamspeakServer server : servers)
            if (server.isEnabled() && !server.isConnected()) server.connect();
    }

    @Override
    public void disconnect() {
        Collection<TeamspeakServer> servers = serverManager.getServers();
        for (TeamspeakServer server : servers) {
            if (server.isConnected()) server.disconnect();
        }
    }

    @Override
    public PlatformUser getSelf() {
        return null;
    }

    @Override
    public Collection<String> getPlatformUserIds() {
        return null;
    }

    @Override
    public Collection<String> getChatIds() {
        return null;
    }
}
