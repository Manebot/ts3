package io.manebot.plugin.ts3.command;

import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.tuple.Pair;

import java.util.function.Consumer;

public enum TeamspeakConnectionProperty {
    NICKNAME("nickname", (pair) -> pair.getLeft().setDisplayName(pair.getRight())),
    IDLE_TIMEOUT("idle-timeout", (pair) -> pair.getLeft().setIdleTimeout(Integer.parseInt(pair.getRight()))),
    AWAY_CHANNEL("away-channel", (pair) -> pair.getLeft().setAwayChannel(pair.getRight())),
    LOBBY_CHANNEL("lobby-channel", (pair) -> pair.getLeft().setLobbyChannel(pair.getRight())),
    PASSWORD("password", (pair) -> pair.getLeft().setPassword(pair.getRight())),
    FOLLOW("follow", (pair) -> pair.getLeft().setFollow(Boolean.parseBoolean(pair.getRight())));

    private final Consumer<Pair<TeamspeakServer, String>> setter;
    private final String name;

    TeamspeakConnectionProperty(String name, Consumer<Pair<TeamspeakServer, String>> setter) {
        this.name = name;
        this.setter = setter;
    }

    public String getName() {
        return name;
    }

    public Consumer<Pair<TeamspeakServer, String>> getSetter() {
        return setter;
    }

    public static TeamspeakConnectionProperty fromName(String propertyKey) {
        for (TeamspeakConnectionProperty property : values())
             if (property.getName().equalsIgnoreCase(propertyKey)) return property;

        throw new IllegalArgumentException("Property not recognized.");
    }
}