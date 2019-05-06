package io.manebot.plugin.ts3.platform.chat;

import io.manebot.chat.Chat;
import io.manebot.chat.ChatMessage;
import io.manebot.chat.TextFormat;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;
import io.manebot.plugin.ts3.platform.server.TeamspeakServerConnection;
import io.manebot.tuple.Pair;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TeamspeakServerChat extends TeamspeakChat implements Chat {
    public TeamspeakServerChat(TeamspeakPlatformConnection platformConnection,
                               TeamspeakServer server) {
        super(platformConnection, server, TeamspeakChat.getServerChatId(server));
    }

    @Override
    public void setName(String name) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not done yet"); //TODO
    }

    @Override
    public void removeMember(String platformId) {
        // Channel kick
        throw new UnsupportedOperationException("not done yet"); //TODO
    }

    @Override
    public void addMember(String platformId) {
        // Move to channel
        throw new UnsupportedOperationException("not done yet"); //TODO
    }

    @Override
    public Collection<PlatformUser> getPlatformUsers() {
        TeamspeakServerConnection serverConnection = getServer().getConnection();
        if (serverConnection == null || !serverConnection.isConnected())
            return Collections.emptyList();

        return serverConnection.getRegisteredClients().stream().map(Pair::getRight).collect(Collectors.toList());
    }

    @Override
    protected void sendSingleMessage(String rawMessage) throws IOException {
        TeamspeakServerConnection serverConnection = getServer().getConnection();
        if (serverConnection == null || !serverConnection.isConnected())
            throw new IOException("not connected to Teamspeak3 server \"" + getServer().getId() + "\"");

        serverConnection.sendServerMessage(rawMessage);
    }
}
