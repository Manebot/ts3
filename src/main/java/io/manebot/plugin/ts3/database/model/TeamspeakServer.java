package io.manebot.plugin.ts3.database.model;

import io.manebot.database.Database;
import io.manebot.database.model.TimedRow;
import io.manebot.plugin.ts3.platform.server.TeamspeakServerConnection;

import javax.persistence.*;

@javax.persistence.Entity
@Table(
        indexes = {
                @Index(columnList = "endpoint", unique = true),
                @Index(columnList = "enabled")
        },
        uniqueConstraints = {@UniqueConstraint(columnNames ={"endpoint"})}
)
public class TeamspeakServer extends TimedRow {
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

    @Column()
    private String endpoint;

    @Column(nullable = true)
    private String displayName;

    @Column(nullable = true)
    private String password;

    @Column(nullable = true)
    private String lobbyChannel;

    @Column(nullable = true)
    private String awayChannel;

    @Column()
    private boolean enabled = true;

    @Column()
    private int idleTimeout = 600;

    public TeamspeakServer(Database database) {
        this.database = database;
    }

    public TeamspeakServer(Database database, String id) {
        this(database);

        this.id = id;
    }

    public String getId() {
        return id;
    }

    public int getIdleTimeout() {
        return Math.max(0, idleTimeout);
    }

    public void setIdleTimeout(int idleTimeout) {
        if (this.idleTimeout != idleTimeout) {
            this.idleTimeout = database.execute(s -> {
                TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                model.idleTimeout = idleTimeout;
                model.setUpdated(System.currentTimeMillis());
                return idleTimeout;
            });
        }
    }

    public void remove() {
        database.execute(s -> { s.remove(TeamspeakServer.this); });
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (this.displayName == null || !this.displayName.equals(displayName)) {
            this.displayName = database.execute(s -> {
                TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                model.displayName = displayName;
                model.setUpdated(System.currentTimeMillis());
                return displayName;
            });
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        if (this.endpoint == null || !this.endpoint.equals(endpoint)) {
            this.endpoint = database.execute(s -> {
                TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                model.endpoint = endpoint;
                model.setUpdated(System.currentTimeMillis());
                return endpoint;
            });
        }
    }

    public boolean isConnected() {
        TeamspeakServerConnection connection = getConnection();
        return connection != null && connection.isConnected();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (!this.enabled == enabled) {
            this.enabled = database.execute(s -> {
                TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                model.enabled = enabled;
                model.setUpdated(System.currentTimeMillis());
                return enabled;
            });
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (this.password == null || !this.password.equals(password)) {
            this.password = database.execute(s -> {
                TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                model.password = password;
                model.setUpdated(System.currentTimeMillis());
                return password;
            });
        }
    }

    public String getLobbyChannel() {
        return lobbyChannel;
    }

    public void setLobbyChannel(String lobbyChannel) {
        if (this.lobbyChannel == null || !this.lobbyChannel.equals(lobbyChannel)) {
            this.lobbyChannel = database.execute(s -> {
                TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                model.lobbyChannel = lobbyChannel;
                model.setUpdated(System.currentTimeMillis());
                return lobbyChannel;
            });
        }
    }

    public String getAwayChannel() {
        return awayChannel;
    }

    public void setAwayChannel(String awayChannel) {
        if (this.awayChannel == null || !this.awayChannel.equals(awayChannel)) {
            this.awayChannel = database.execute(s -> {
                TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                model.awayChannel = awayChannel;
                model.setUpdated(System.currentTimeMillis());
                return awayChannel;
            });
        }
    }

    public TeamspeakServerConnection getConnection() {
        return connection;
    }

    public void setConnection(TeamspeakServerConnection connection) {
        this.connection = connection;
    }

    public TeamspeakServerConnection connect() {
        TeamspeakServerConnection serverConnection = connection;
        if (serverConnection == null) throw new IllegalStateException("");

        serverConnection.connectAsync().exceptionally((e) -> connect());

        return serverConnection;
    }
}
