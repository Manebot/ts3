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

    public TeamspeakServer addServer(String id, String endpoint) {
        try {
            return database.executeTransaction(s -> {
                TeamspeakServer discordGuild = new TeamspeakServer(database, id, endpoint);
                s.persist(discordGuild);
                return discordGuild;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public TeamspeakServer removeServer(String id) {
        TeamspeakServer server = getServer(id);
        if (server != null) server.remove();
        return server;
    }

    public Collection<TeamspeakServer> getServers() {
        return database.execute(s -> {
            return s.createQuery(
                    "SELECT x FROM " + TeamspeakServer.class.getName() + " x",
                    TeamspeakServer.class
            ).getResultList();
        });
    }

    @Override
    public void load(Plugin.Future future) {}

    @Override
    public void unload(Plugin.Future future) {}
}
