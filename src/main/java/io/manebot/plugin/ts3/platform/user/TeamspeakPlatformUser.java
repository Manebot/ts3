package io.manebot.plugin.ts3.platform.user;

import com.github.manevolent.ts3j.identity.Uid;
import io.manebot.chat.Chat;
import io.manebot.platform.Platform;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakClient;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TeamspeakPlatformUser implements PlatformUser {
    private final TeamspeakPlatformConnection connection;
    private final Uid uid;

    public TeamspeakPlatformUser(TeamspeakPlatformConnection connection, Uid uid) {
        this.connection = connection;
        this.uid = uid;
    }

    public Uid getUid() {
        return uid;
    }

    public Collection<TeamspeakClient> getClients() {
        return connection.findClients(uid).collect(Collectors.toList());
    }

    public TeamspeakClient getLastActiveClient() {
        return connection.findClients(uid)
                .max(Comparator.comparingLong(TeamspeakClient::getLastActivity))
                .orElse(null);
    }

    @Override
    public Platform getPlatform() {
        return connection.getPlatform();
    }

    public String getNickname() {
        String nickname = connection.findClients(uid)
                .map(TeamspeakClient::getNickname)
                .distinct()
                .sorted(String::compareTo)
                .collect(Collectors.joining("/"));

        if (nickname != null && !nickname.isEmpty()) return nickname;
        else return getId();
    }

    @Override
    public boolean isConnected() {
        return connection.findClients(uid).findAny().isPresent();
    }

    @Override
    public String getId() {
        return uid.toBase64();
    }

    @Override
    public boolean isSelf() {
        return connection.getSelf() == this;
    }

    @Override
    public Collection<Chat> getChats() {
        return connection.findClients(uid)
                .flatMap(client -> Stream.of(client.getPrivateChat(), client.getChannelChat()).filter(Objects::nonNull))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Status getStatus() {
        TeamspeakClient client = getLastActiveClient();
        if (client == null) return Status.OFFLINE;

        return Status.ONLINE;
    }

    public Chat getPrivateChat() {
        TeamspeakClient client = getLastActiveClient();
        if (client != null) return client.getPrivateChat();
        else return null;
    }
}
