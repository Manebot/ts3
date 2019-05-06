package io.manebot.plugin.ts3.platform.chat;

import io.manebot.chat.Chat;

import io.manebot.platform.PlatformUser;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;
import io.manebot.plugin.ts3.platform.server.TeamspeakServerConnection;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakChannel;
import io.manebot.tuple.Pair;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class TeamspeakChannelChat extends TeamspeakChat implements Chat {
    private final int channelId;

    public TeamspeakChannelChat(TeamspeakPlatformConnection platformConnection,
                                TeamspeakServer server,
                                int channelId) {
        super(platformConnection, server, getChannelChatId(server, channelId));
        this.channelId = channelId;
    }

    public int getChannelId() {
        return channelId;
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

        TeamspeakChannel channel = serverConnection.findChannelById(getChannelId());
        if (channel == null) throw new IllegalArgumentException(
                "Channel not found in Teamspeak3 server \"" + getServer().getId() + "\": " + getChannelId()
        );

        return serverConnection.getRegisteredClients(channel.getClients()).stream()
                .map(Pair::getRight).collect(Collectors.toList());
    }

    @Override
    protected void sendSingleMessage(String rawMessage) throws IOException {
        TeamspeakServerConnection serverConnection = getServer().getConnection();
        if (serverConnection == null || !serverConnection.isConnected())
            throw new IOException("not connected to Teamspeak3 server \"" + getServer().getId() + "\"");

        serverConnection.sendChannelMessage(serverConnection.findChannelById(getChannelId()), rawMessage);
    }
}
