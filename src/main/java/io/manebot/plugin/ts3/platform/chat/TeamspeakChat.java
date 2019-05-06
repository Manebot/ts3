package io.manebot.plugin.ts3.platform.chat;

import com.github.manevolent.ts3j.identity.Uid;
import io.manebot.chat.Chat;
import io.manebot.chat.ChatMessage;
import io.manebot.chat.TextFormat;
import io.manebot.platform.Platform;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakChannel;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakClient;

import java.util.Collection;

public abstract class TeamspeakChat implements Chat {
    private final TeamspeakPlatformConnection platformConnection;
    private final TeamspeakServer server;
    private final String id;

    public TeamspeakChat(TeamspeakPlatformConnection platformConnection,
                         TeamspeakServer server, String id) {
        this.platformConnection = platformConnection;
        this.server = server;
        this.id = id;
    }

    public TeamspeakPlatformConnection getPlatformConnection() {
        return platformConnection;
    }

    public final TeamspeakServer getServer() {
        return server;
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final Platform getPlatform() {
        return platformConnection.getPlatform();
    }

    @Override
    public boolean isConnected() {
        return server.isConnected();
    }

    @Override
    public Collection<ChatMessage> getLastMessages(int max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canChangeTypingStatus() {
        return false;
    }

    @Override
    public void setTyping(boolean typing) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTyping() {
        return false;
    }

    @Override
    public TextFormat getFormat() {
        return TeamspeakTextFormat.INSTANCE;
    }

    @Override
    public boolean canSendEmbeds() {
        return false;
    }

    @Override
    public boolean isBuffered() {
        return false;
    }

    public static final String getChannelChatId(TeamspeakServer server, int channelId) {
        return server.getId().toLowerCase() + ":channel:" + Integer.toString(channelId);
    }

    public static final String getChannelChatId(TeamspeakServer server, TeamspeakChannel channel) {
        return getChannelChatId(server, channel.getChannelId());
    }

    public static final String getServerChatId(TeamspeakServer server) {
        return server.getId().toLowerCase() + ":global";
    }

    public static final String getPrivateChatId(TeamspeakServer server, Uid uid) {
        return server.getId().toLowerCase() + ":private:" + uid.toBase64();
    }

    public static final String getPrivateChatId(TeamspeakServer server, TeamspeakClient client) {
        return getPrivateChatId(server, client.getUid());
    }
}
