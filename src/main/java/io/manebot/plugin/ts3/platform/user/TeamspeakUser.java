package io.manebot.plugin.ts3.platform.user;

import io.manebot.chat.Chat;
import io.manebot.platform.Platform;
import io.manebot.platform.PlatformUser;

import java.util.Collection;

public class TeamspeakUser implements PlatformUser {
    @Override
    public Platform getPlatform() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public boolean isSelf() {
        return false;
    }

    @Override
    public Collection<Chat> getChats() {
        return null;
    }
}
