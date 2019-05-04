package io.manebot.plugin.ts3.database.model;

import io.manebot.database.Database;
import io.manebot.database.model.Conversation;
import io.manebot.database.model.TimedRow;
import io.manebot.plugin.ts3.platform.server.TeamspeakServerConnection;

import javax.persistence.*;
import java.io.IOException;

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
        if (!this.displayName.equals(displayName)) {
            this.displayName = database.execute(s -> {
                TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                model.displayName = displayName;
                return displayName;
            });
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        if (!this.endpoint.equals(endpoint)) {
            this.endpoint = database.execute(s -> {
                TeamspeakServer model = s.find(TeamspeakServer.class, teamspeakServerId);
                model.endpoint = endpoint;
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
                return enabled;
            });
        }
    }

    public TeamspeakServerConnection getConnection() {
        return connection;
    }

    public void setConnection(TeamspeakServerConnection connection) {
        this.connection = connection;
    }

    public void connect() throws IOException {
        TeamspeakServerConnection connection = getConnection();
        if (connection == null)
            connection = new TeamspeakServerConnection(this);

        connection.connect();

        setConnection(connection);
    }

    public void disconnect() throws IOException {
        TeamspeakServerConnection connection = getConnection();
        if (connection != null) {
            connection.disconnect();
            setConnection(null);
        }
    }
}
