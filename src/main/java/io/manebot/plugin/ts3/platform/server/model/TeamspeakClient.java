package io.manebot.plugin.ts3.platform.server.model;

import com.github.manevolent.ts3j.identity.Uid;
import io.manebot.chat.Chat;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.ts3.platform.server.TeamspeakServerConnection;
import io.manebot.user.UserAssociation;

public class TeamspeakClient {
    private final TeamspeakServerConnection connection;
    private final int clientId;
    private final Uid uid;

    private String nickname;
    private int channelId = -1;
    private TeamspeakChannel channel;
    private boolean listening;
    private long lastActivity = System.currentTimeMillis();

    public TeamspeakClient(TeamspeakServerConnection connection, int clientId, Uid uid) {
        this.connection = connection;
        this.clientId = clientId;
        this.uid = uid;
    }

    public TeamspeakServerConnection getConnection() {
        return connection;
    }
    public int getClientId() {
        return clientId;
    }
    public PlatformUser getPlatformUser() {
        return getConnection().getPlatformUser(this);
    }

    public TeamspeakChannel getChannel() {
        return channel;
    }
    public void setChannel(TeamspeakChannel channel) {
        this.channel = channel;
    }

    public Uid getUid() {
        return uid;
    }

    public int getChannelId() {
        return channelId;
    }
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isListening() {
        return listening;
    }
    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public long getLastActivity() {
        return lastActivity;
    }
    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public Chat getPrivateChat() {
        return connection.getPrivateChat(uid);
    }

    public Chat getChannelChat() {
        return channel == null ? null : connection.getChannelChat(channel);
    }
}
