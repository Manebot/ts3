package io.manebot.plugin.ts3.platform.server;

import io.manebot.database.Database;
import io.manebot.plugin.Plugin;
import io.manebot.plugin.PluginReference;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;

import java.sql.SQLException;
import java.util.Collection;

public class ServerManager implements PluginReference {
    private final Database database;

    public ServerManager(Database database) {
        this.database = database;
    }

    public TeamspeakServer getServer(String id) {
        return database.execute(s -> {
            return s.createQuery(
                    "SELECT x FROM " + TeamspeakServer.class.getName() + " x WHERE x.id=:id",
                    TeamspeakServer.class
            ).setParameter("id", id)
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElse(null);
        });
    }

    public TeamspeakServer getOrCreateServer(String id) {
        try {
            return database.executeTransaction(s -> {
                return s.createQuery(
                        "SELECT x FROM " + TeamspeakServer.class.getName() + " x WHERE x.id=:id",
                        TeamspeakServer.class
                ).setParameter("id", id)
                        .getResultList()
                        .stream()
                        .findFirst()
                        .orElseGet(() -> {
                            TeamspeakServer discordGuild = new TeamspeakServer(database, id);
                            s.persist(discordGuild);
                            return discordGuild;
                        });
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeServer(String stringID) {
        TeamspeakServer guild = getServer(stringID);
        if (guild != null) guild.remove();
    }

    @Override
    public void load(Plugin.Future future) {}

    @Override
    public void unload(Plugin.Future future) {}

    public Collection<TeamspeakServer> getServers() {
        return database.execute(s -> {
            return s.createQuery(
                    "SELECT x FROM " + TeamspeakServer.class.getName() + " x WHERE x.id=:id",
                    TeamspeakServer.class
            ).getResultList();
        });
    }
}
