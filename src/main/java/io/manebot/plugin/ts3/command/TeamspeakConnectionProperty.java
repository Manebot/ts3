package io.manebot.plugin.ts3.command;

public enum TeamspeakConnectionProperty {
    NICKNAME("nickname"),
    IDLE_TIMEOUT("idle-timeout"),
    AWAY_CHANNEL("away-channel"),
    LOBBY_CHANNEL("lobby-channel"),
    PASSWORD("password");

    private final String name;

    TeamspeakConnectionProperty(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}