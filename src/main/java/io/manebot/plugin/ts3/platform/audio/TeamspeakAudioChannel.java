package io.manebot.plugin.ts3.platform.audio;

import io.manebot.chat.Chat;
import io.manebot.plugin.audio.Audio;
import io.manebot.plugin.audio.api.AbstractAudioConnection;
import io.manebot.plugin.audio.channel.AudioChannel;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.chat.TeamspeakChannel;
import io.manebot.plugin.ts3.platform.server.TeamspeakServerConnection;

public class TeamspeakAudioChannel extends AbstractAudioConnection {
    private final TeamspeakServerConnection connection;

    private TeamspeakAudioChannel(Audio audio, TeamspeakServerConnection connection) {
        super(audio);

        this.connection = connection;
    }

    @Override
    public AudioChannel getChannel(Chat chat) {
        if (chat instanceof TeamspeakChannel) {
            TeamspeakServer server = ((TeamspeakChannel) chat).getServer();
            if (!server.isEnabled())
                return null;

            TeamspeakServerConnection connection = server.getConnection();
            if (connection == null || !connection.isConnected())
                return null;

            return connection.getAudioChannel();
        } else return null;
    }

    @Override
    public boolean isConnected() {
        return super.isConnected() && connection.isConnected();
    }
}