package io.manebot.plugin.ts3.platform.server;

import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.audio.TeamspeakAudioChannel;

import java.io.IOException;

public class TeamspeakServerConnection {
    private final TeamspeakServer server;
    private final TeamspeakAudioChannel audioChannel;

    public TeamspeakServerConnection(TeamspeakServer server) {
        this.server = server;

        audioChannel = new TeamspeakAudioChannel()
    }

    public boolean isConnected() {
        return false;
    }

    public void connect() throws IOException, IllegalStateException {

    }

    public void disconnect() throws IOException, IllegalStateException {

    }
}
