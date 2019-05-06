package io.manebot.plugin.ts3.platform.chat;

import com.github.manevolent.ts3j.identity.Uid;
import io.manebot.chat.Chat;
import io.manebot.chat.ChatMessage;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;

import java.util.Collection;
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
        return null;
    }

    @Override
    public boolean isPrivate() {
        return true;
    }

    @Override
    public Collection<ChatMessage> sendMessage(Consumer<ChatMessage.Builder> function) {
        return null;
    }
}
