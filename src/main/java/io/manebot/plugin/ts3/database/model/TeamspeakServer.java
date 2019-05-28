package io.manebot.plugin.ts3.database.model;

import io.manebot.chat.Chat;
import io.manebot.chat.Community;
import io.manebot.database.Database;
import io.manebot.database.model.TimedRow;
import io.manebot.platform.Platform;
import io.manebot.platform.PlatformConnection;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.ts3.platform.chat.TeamspeakChat;
import io.manebot.plugin.ts3.platform.server.TeamspeakServerConnection;

import javax.persistence.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@javax.persistence.Entity
@Table(
        indexes = {
                @Index(columnList = "endpoint", unique = true),
                @Index(columnList = "enabled")
        },
        uniqueConstraints = {@UniqueConstraint(columnNames ={"endpoint"})}
)
public class TeamspeakServer extends TimedRow implements Community {
    @Transient
    private final io.manebot.database.Database database;

    @Transient
    private TeamspeakServerConnection connection;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column()
    private int teamspeakServerId;

    @Column()
    private String id;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = true)
    private String displayName;

    @Column(nullable = true)
    private String password;

    @Column(nullable = true)
    private String lobbyChannel;

    @Column(nullable = true)
    private String awayChannel;

    @Column(nullable = false)
    private boolean follow = true;

    @Column()
    private boolean enabled = false;

    @Column()
    private int idleTimeout = 600;

    public TeamspeakServer(Database database, String id, String endpoint) {
        this.database = database;
        this.id = id;
        this.endpoint = endpoint;
    }

    public TeamspeakServer(Database database) {
        this.database = database;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setName(String name) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Platform getPlatform() {
        return database.getDatabaseManager().getBot().getPlatformById("ts3");
    }

    @Override
    public Collection<String> getChatIds() {
        TeamspeakServerConnection connection = this.connection;
        if (connection == null || !connection.isConnected()) return Collections.emptyList();

        List<String> chats = new ArrayList<>();

        chats.addAll(connection.getChannels().stream()
                .map(channel -> TeamspeakChat.getChannelChatId(this, channel))
                .collect(Collectors.toList()));

        chats.addAll(connection.getClients().stream()
                .map(client -> TeamspeakChat.getPrivateChatId(this, client))
                .collect(Collectors.toList()));

        chats.add(TeamspeakChat.getServerChatId(this));

        return chats;
    }

    @Override
    public Collection<Chat> getChats() {
        PlatformConnection connection = getPlatform().getConnection();
        if (connection == null || !connection.isConnected())
            return Collections.emptyList();
        return getChatIds().stream().map(connection::getChat).collect(Collectors.toList());
    }

    @Override
    public Collection<String> getPlatformUserIds() {
        TeamspeakServerConnection connection = this.connection;
        if (connection == null || !connection.isConnected()) return Collections.emptyList();
        return connection.getClients().stream().map(client -> client.getUid().toBase64()).collect(Collectors.toList());
    }

    @Override
    public Collection<PlatformUser> getPlatformUsers() {
        PlatformConnection connection = getPlatform().getConnection();
        if (connection == null || !connection.isConnected())
            return Collections.emptyList();
        return getPlatformUserIds().stream().map(connection::getPlatformUser).collect(Collectors.toList());
    }

    @Override
    public Chat getDefaultChat() {
        PlatformConnection connection = getPlatform().getConnection();
        if (connection == null || !connection.isConnected())
            return null;
        return connection.getChat(TeamspeakChat.getServerChatId(this));
    }

    public int getIdleTimeout() {
        return Math.max(0, idleTimeout);
    }

    public void setIdleTimeout(int idleTimeout) {
        if (this.idleTimeout != idleTimeout) {
            try {
                this.idleTimeout = database.executeTransaction(s -> {
                    TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                    model.idleTimeout = idleTimeout;
                    model.setUpdated(System.currentTimeMillis());
                    return idleTimeout;
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            TeamspeakServerConnection serverConnection = getConnection();
            if (serverConnection != null && serverConnection.isConnected()) {
                serverConnection.onIdleTimeoutChanged(idleTimeout);
            }
        }
    }

    public void remove() {
        try {
            database.executeTransaction(s -> { s.remove(TeamspeakServer.this); });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (this.displayName == null || !this.displayName.equals(displayName)) {
            try {
                this.displayName = database.executeTransaction(s -> {
                    TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                    model.displayName = displayName;
                    model.setUpdated(System.currentTimeMillis());
                    return displayName;
                });

                TeamspeakServerConnection serverConnection = getConnection();
                if (serverConnection != null && serverConnection.isConnected()) {
                    serverConnection.onNicknameChanged(displayName);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public boolean isConnected() {
        TeamspeakServerConnection connection = getConnection();
        return connection != null && connection.isConnected();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (!this.enabled == enabled) {
            try {
                this.enabled = database.executeTransaction(s -> {
                    TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                    model.enabled = enabled;
                    model.setUpdated(System.currentTimeMillis());
                    return enabled;
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            if (!enabled && isConnected())
                getConnection().disconnectAsync();
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (this.password == null || !this.password.equals(password)) {
            try {
                this.password = database.executeTransaction(s -> {
                    TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                    model.password = password;
                    model.setUpdated(System.currentTimeMillis());
                    return password;
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            TeamspeakServerConnection serverConnection = getConnection();
            if (serverConnection != null && serverConnection.isConnected()) {
                serverConnection.onPasswordChanged(password);
            }
        }
    }

    public String getLobbyChannel() {
        return lobbyChannel;
    }

    public void setLobbyChannel(String lobbyChannel) {
        if (this.lobbyChannel == null || !this.lobbyChannel.equals(lobbyChannel)) {
            try {
                this.lobbyChannel = database.executeTransaction(s -> {
                    TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                    model.lobbyChannel = lobbyChannel;
                    model.setUpdated(System.currentTimeMillis());
                    return lobbyChannel;
                });

                TeamspeakServerConnection serverConnection = getConnection();
                if (serverConnection != null && serverConnection.isConnected()) {
                    serverConnection.onLobbyChannelChanged(lobbyChannel);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public String getAwayChannel() {
        return awayChannel;
    }

    public void setAwayChannel(String awayChannel) {
        if (this.awayChannel == null || !this.awayChannel.equals(awayChannel)) {
            try {
                this.awayChannel = database.executeTransaction(s -> {
                    TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                    model.awayChannel = awayChannel;
                    model.setUpdated(System.currentTimeMillis());
                    return awayChannel;
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            TeamspeakServerConnection serverConnection = getConnection();
            if (serverConnection != null && serverConnection.isConnected()) {
                serverConnection.onAwayChannelChanged(awayChannel);
            }
        }
    }

    public TeamspeakServerConnection getConnection() {
        return connection;
    }

    public void setConnection(TeamspeakServerConnection connection) {
        this.connection = connection;
    }

    public TeamspeakServerConnection connectAsync() {
        TeamspeakServerConnection serverConnection = connection;
        if (serverConnection == null) throw new NullPointerException();

        if (!isEnabled()) throw new IllegalStateException();

        serverConnection.connectAsync().exceptionally((e) -> {
            Logger.getGlobal().log(Level.WARNING, "Problem connecting to Teamspeak3 server \"" + id + "\"", e);

            try {
                Thread.sleep(10_000L);
            } catch (InterruptedException ex) {
                return serverConnection;
            }

            if (isEnabled()) return connectAsync();
            else return serverConnection;
        });

        return serverConnection;
    }

    public void setFollow(boolean follow) {
        if (this.follow != follow) {
            try {
                this.follow = database.executeTransaction(s -> {
                    TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                    model.follow = false;
                    model.setUpdated(System.currentTimeMillis());
                    return false;
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean willFollow() {
        return follow;
    }
}
