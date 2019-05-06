package io.manebot.plugin.ts3.platform.chat;

import io.manebot.chat.Chat;
import io.manebot.chat.DefaultTextBuilder;
import io.manebot.chat.TextBuilder;

public class TeamspeakTextBuilder extends DefaultTextBuilder {
    public TeamspeakTextBuilder(Chat chat) {
        super(chat, TeamspeakTextFormat.INSTANCE);
    }

    @Override
    public TextBuilder appendUrl(String url) {
        return appendRaw("[URL]" + getFormat().escape(url) + "[/URL]");
    }
}
