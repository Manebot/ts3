package io.manebot.plugin.ts3.platform.chat;

import io.manebot.chat.Chat;
import io.manebot.chat.ChatMessage;
import io.manebot.chat.TextFormat;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;

import java.util.Collection;
import java.util.function.Consumer;

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
        return null;
    }

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public Collection<ChatMessage> sendMessage(Consumer<ChatMessage.Builder> function) {
        return null;
    }
}
