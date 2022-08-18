package io.manebot.plugin.ts3.platform.audio.voice;

import io.manebot.platform.PlatformUser;
import io.manebot.plugin.audio.channel.AudioChannel;
import io.manebot.plugin.audio.mixer.input.AudioProvider;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakClient;
import io.manebot.user.User;
import io.manebot.user.UserAssociation;

import java.io.IOException;
import java.util.logging.Logger;

public class AudioFeedListener extends VoiceListener implements AudioProvider {
    private final AudioChannel channel;
    private final float[] buffer;
    private int position = 0;
    private long packetsReceived = 0;
    private long underflowed = 0, overflowed = 0;
    private long written = 0; // total sent to child listeners
    private boolean connected = false;

    public AudioFeedListener(AudioChannel channel, TeamspeakClient client, int sampleRate, int channels)
            throws IOException {
        super(client, sampleRate, channels);
        this.channel = channel;

        int bufferSize = sampleRate * channels;

        this.buffer = new float[bufferSize];
    }

    public long getPacketsReceived() {
        return packetsReceived;
    }

    public int getPosition() {
        return position;
    }

    public long getUnderflowed() {
        return underflowed;
    }

    public long getOverflowed() {
        return overflowed;
    }

    public AudioProvider open() {
        return this;
    }

    @Override
    protected void receive(int packetId, float[] buffer) throws IOException {
        synchronized (this.buffer) {
            if (hasEnded()) return;

            if (this.buffer.length - position < buffer.length) {
                overflowed ++;

                // throw away n smps
                int delete = Math.max(Math.min(buffer.length, buffer.length - (this.buffer.length - position)), 0);
                if (delete > 0)
                    System.arraycopy(this.buffer, delete, this.buffer, 0, position - delete);

                position -= delete;
            }

            System.arraycopy(buffer, 0, this.buffer, position, buffer.length);

            this.position += buffer.length;
        }

        if (!connected) connect();
    }

    @Override
    public void onEnd() {
        if (position <= 0) disconnect();
    }

    @Override
    public int available() {
        synchronized (buffer) {
            return position;
        }
    }

    private User getUser() {
        PlatformUser platformUser = getClient().getPlatformUser();
        if (platformUser == null) return null;

        UserAssociation association = platformUser.getAssociation();
        if (association == null) return null;

        return association.getUser();
    }

    private void connect() {
        if (!connected) {
            channel.setProvider(getClient().getPlatformUser(), this);

            connected = true;

            Logger.getGlobal().fine(
                    "Connected " + getClient().getNickname() + " to audio feed."
            );
        }
    }

    private void disconnect() {
        if (connected) {
            channel.removeProvider(getClient().getPlatformUser());

            connected = false;

            Logger.getGlobal().fine(
                    "Disconnected " + getClient().getNickname() + " audio feed; " +
                            "written=" + written + ", pos=" + position
            );
        }
    }

    @Override
    public int read(float[] buffer, int offs, int len) throws IOException {
        try {
            synchronized (this.buffer) {
                int available = available();

                int real = Math.min(available, len);
                if (real < available) underflowed++;

                if (real > 0)
                    System.arraycopy(this.buffer, 0, buffer, offs, real);

                if (real > 0 && available - real > 0)
                    System.arraycopy(this.buffer, real, this.buffer, 0, available - real);

                this.position -= real;

                this.written += real;

                return real;
            }
        } finally {
            if (this.position <= 0 && hasEnded()) disconnect();
        }
    }

    @Override
    public void close() throws IOException {
        end();
    }

    @Override
    public String toString() {
        return "Feed{" + getClient().getNickname() + "}";
    }
}