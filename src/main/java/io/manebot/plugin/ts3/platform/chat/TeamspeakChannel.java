package io.manebot.plugin.ts3.platform.chat;

import io.manebot.chat.Chat;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;

public abstract class TeamspeakChannel implements Chat {
    private final TeamspeakServer server;

    protected TeamspeakChannel(TeamspeakServer server) {
        this.server = server;
    }

    public TeamspeakServer getServer() {
        return server;
    }
}
