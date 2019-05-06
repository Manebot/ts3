package io.manebot.plugin.ts3.platform.server.model;

import io.manebot.plugin.ts3.platform.server.TeamspeakServerConnection;

import java.util.*;

public class TeamspeakChannel {
    private final TeamspeakServerConnection connection;
    private final int channelId;

    private final List<TeamspeakClient> clients = new LinkedList<>();

    private String name;

    public TeamspeakChannel(TeamspeakServerConnection connection, int channelId) {
        this.connection = connection;
        this.channelId = channelId;
    }

    public void addClient(TeamspeakClient client) {
        if (!this.clients.contains(client))
            this.clients.add(client);
    }

    public void removeClient(TeamspeakClient client) {
        this.clients.remove(client);
    }

    public Collection<TeamspeakClient> getClients() {
        return Collections.unmodifiableCollection(new ArrayList<>(clients));
    }

    public int getChannelId() {
        return channelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TeamspeakServerConnection getConnection() {
        return connection;
    }
}
