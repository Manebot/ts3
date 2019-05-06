package io.manebot.plugin.ts3.platform.chat;

import com.github.manevolent.ts3j.identity.Uid;
import io.manebot.chat.BasicTextChatMessage;
import io.manebot.chat.Chat;
import io.manebot.chat.ChatMessage;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;
import io.manebot.plugin.ts3.platform.server.TeamspeakServerConnection;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

public class TeamspeakPrivateChat extends TeamspeakChat implements Chat {
    private final Uid uid;

    public TeamspeakPrivateChat(TeamspeakPlatformConnection platformConnection,
                                TeamspeakServer server,
                                Uid uid) {
        super(platformConnection, server, TeamspeakChat.getServerChatId(server));
        this.uid = uid;
    }

    public Uid getUid() {
        return uid;
    }

    @Override
    public void setName(String name) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeMember(String platformId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMember(String platformId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<PlatformUser> getPlatformUsers() {
        TeamspeakServerConnection serverConnection = getServer().getConnection();
        if (serverConnection == null || !serverConnection.isConnected())
            return Collections.emptyList();

        return Collections.singletonList(getPlatformConnection().getPlatformUser(getUid()));
    }

    @Override
    public boolean isPrivate() {
        return true;
    }

    @Override
    protected void sendSingleMessage(String rawMessage) throws IOException {
        TeamspeakServerConnection serverConnection = getServer().getConnection();
        if (serverConnection == null || !serverConnection.isConnected())
            throw new IOException("not connected to Teamspeak3 server \"" + getServer().getId() + "\"");

        serverConnection.sendPrivateMessage(getUid(), rawMessage);
    }
}
