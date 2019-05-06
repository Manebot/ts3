package io.manebot.plugin.ts3.platform.chat;

import com.github.manevolent.ts3j.identity.Uid;
import io.manebot.chat.*;
import io.manebot.platform.Platform;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakChannel;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakClient;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

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

    @Override
    public boolean canFormatMessages() { return true; }

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public TextBuilder text() {
        return new TeamspeakTextBuilder(this);
    }

    protected abstract void sendSingleMessage(String rawMessage) throws IOException;

    private Collection<ChatMessage> sendMessage(TeamspeakChatMessage.Builder chatMessage) {
        String rawMessage = chatMessage.getMessage();

        try {
            sendSingleMessage(rawMessage);
        } catch (IOException e) {
            throw new RuntimeException("Problem sending Teamspeak3 chat message", e);
        }

        return Collections.singletonList(chatMessage.build());
    }

    @Override
    public Collection<ChatMessage> sendMessage(Consumer<ChatMessage.Builder> function) {
        TeamspeakChatMessage.Builder builder = new TeamspeakChatMessage.Builder(
                getPlatformConnection(),
                getServer(),
                getPlatformConnection().getSelf(),
                this
        );

        function.accept(builder);

        return sendMessage(builder);
    }

    public static String getChannelChatId(TeamspeakServer server, int channelId) {
        return server.getId().toLowerCase() + ":channel:" + Integer.toString(channelId);
    }

    public static String getChannelChatId(TeamspeakServer server, TeamspeakChannel channel) {
        return getChannelChatId(server, channel.getChannelId());
    }

    public static String getServerChatId(TeamspeakServer server) {
        return server.getId().toLowerCase() + ":global";
    }

    public static String getPrivateChatId(TeamspeakServer server, Uid uid) {
        return server.getId().toLowerCase() + ":private:" + uid.toBase64();
    }

    public static String getPrivateChatId(TeamspeakServer server, TeamspeakClient client) {
        return getPrivateChatId(server, client.getUid());
    }
}
