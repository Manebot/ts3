package io.manebot.plugin.ts3.platform.chat;

import io.manebot.chat.*;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;
import io.manebot.plugin.ts3.platform.user.TeamspeakPlatformUser;

public class TeamspeakChatMessage extends BasicTextChatMessage {
    private final TeamspeakServer server;
    private final TeamspeakPlatformConnection connection;
    private final String message;

    public TeamspeakChatMessage(TeamspeakPlatformConnection connection,
                                TeamspeakServer server,
                                ChatSender sender,
                                String rawMessage) {
        super(sender, rawMessage);

        this.connection = connection;
        this.server = server;
        this.message = stripBBCode(rawMessage);
    }

    public TeamspeakPlatformConnection getPlatformConnection() {
        return connection;
    }

    public TeamspeakServer getServer() {
        return server;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static class Builder extends BasicTextChatMessage.Builder {
        private final TeamspeakPlatformConnection platformConnection;
        private final TeamspeakServer server;
        private final TeamspeakPlatformUser user;
        private final TeamspeakChat chat;

        public Builder(TeamspeakPlatformConnection platformConnection,
                              TeamspeakServer server,
                              TeamspeakPlatformUser user,
                              TeamspeakChat chat) {
            super(user, chat);
            this.platformConnection = platformConnection;
            this.server = server;

            this.user = user;
            this.chat = chat;
        }

        @Override
        public TeamspeakPlatformUser getUser() {
            return user;
        }

        @Override
        public TeamspeakChat getChat() {
            return chat;
        }

        @Override
        public Builder rawMessage(String message) {
            super.rawMessage(message);
            return this;
        }

        public TeamspeakChatMessage build() {
            return new TeamspeakChatMessage(
                    platformConnection,
                    server,
                    new DefaultChatSender(user, chat),
                    getMessage()
            );
        }
    }

    public static String stripBBCode(String rawMessage) {
        return rawMessage.replaceAll("\\[\\/?[A-z]+\\]", "");
    }
}
