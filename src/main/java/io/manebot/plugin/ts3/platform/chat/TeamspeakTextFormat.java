package io.manebot.plugin.ts3.platform.chat;

import io.manebot.chat.TextFormat;
import io.manebot.platform.PlatformUser;

public final class TeamspeakTextFormat implements TextFormat {
    public static final TeamspeakTextFormat INSTANCE = new TeamspeakTextFormat();

    @Override
    public boolean shouldMention(PlatformUser user) {
        return false;
    }
}
