package io.manebot.plugin.ts3.platform.chat;

import io.manebot.chat.DefaultChatSender;
import io.manebot.plugin.ts3.platform.user.TeamspeakPlatformUser;

public class TeamspeakChatSender extends DefaultChatSender {
    private final TeamspeakPlatformUser user;
    private final TeamspeakChat chat;

    public TeamspeakChatSender(TeamspeakPlatformUser user, TeamspeakChat chat) {
        super(user, chat);

        this.user = user;
        this.chat = chat;
    }

    @Override
    public TeamspeakPlatformUser getPlatformUser() {
        return user;
    }

    @Override
    public TeamspeakChat getChat() {
        return chat;
    }
}