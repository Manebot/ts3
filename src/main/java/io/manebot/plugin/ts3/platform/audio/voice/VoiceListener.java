package io.manebot.plugin.ts3.platform.audio.voice;

import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;

import io.manebot.plugin.audio.opus.OpusDecoder;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakClient;
import io.manebot.virtual.Virtual;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class VoiceListener implements Runnable {
    private final int sampleRate;
    private final int channels;
    private final int frameSize;
    private static final int OPUS_FRAME_TIME_MS = 20;

    private final TeamspeakClient client;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final OpusDecoder decoder;
    private final float[] decoderBuffer;

    private final Object watcherLock = new Object();
    private final Thread watcher;

    private long lastPacket;
    private boolean firstPacket = true;
    private boolean ended = false;

    private final AbstractTeamspeakClientSocket.RemoteCounterFull remoteCounter =
            new AbstractTeamspeakClientSocket.RemoteCounterFull(
                    65536,
                    (1000 / OPUS_FRAME_TIME_MS) * 31
            ); // 31 second window (1550 packets)

    public VoiceListener(TeamspeakClient client, int sampleRate, int channels) throws IOException {
        this.client = client;

        this.sampleRate = 16000;
        this.channels = channels;
        this.frameSize = (sampleRate / (1000 / OPUS_FRAME_TIME_MS));

        this.decoder = new OpusDecoder(sampleRate, frameSize, channels);
        this.decoderBuffer =  new float[frameSize * channels];

        this.lastPacket = System.currentTimeMillis();

        this.watcher = new Thread(this);
        this.watcher.setDaemon(true);
        this.watcher.start();
    }

    public TeamspeakClient getClient() {
        return client;
    }

    public boolean hasEnded() {
        return ended;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    protected void onEnd() {

    }

    protected abstract void receive(int packetId, float[] buffer) throws IOException;

    public void receive(int packetId, byte[] encodedData) {
        try {
            boolean wasFirstPacket = false;
            int lastGeneration = remoteCounter.getCurrentGeneration();
            int lastPacketId = remoteCounter.getCurrentPacketId();

            // Put the packet in to increase the counter and also ensure that the packet isn't a resent packet
            if (!remoteCounter.put(packetId)) {
                return; // resent
            }

            // Ensure the packet is ADVANCING FORWARD.  We will fill in ANY lost packets with PLC.
            int lostPackets = 0;

            if (!firstPacket) {
                if (lastGeneration < remoteCounter.getCurrentGeneration()) {
                    lostPackets += lastPacketId - 65536 - 1; // missing packets on right side of old generation
                    lostPackets += packetId; // missing packets on left side of new generation
                    lostPackets = Math.abs(lostPackets);
                } else if (lastPacketId < packetId) {
                    lostPackets = packetId - lastPacketId - 1;
                } else {
                    return; // old packet just now arriving (new, but just arrived)
                }
            } else {
                Logger.getGlobal().log(Level.FINE, "Started listening to " + client.getNickname() + "...");
                wasFirstPacket = true;
                firstPacket = false;
            }

            // At this point, the packet must be processed, so mark the current time so the watcher thread
            // doesn't freak out and end us
            synchronized (watcherLock) {
                if (ended) return; // watcher may have killed it by this point

                lastPacket = System.currentTimeMillis();
                watcherLock.notifyAll();
            }

            // Check for lost packets
            if (lostPackets < 0) // weird situation to be in, but uh, okay
                throw new IllegalArgumentException("lostPackets < 0: " + Integer.toString(lostPackets));

            // if we lose to many packets then just die, man
            if (lostPackets > (1000 / OPUS_FRAME_TIME_MS) * 31)
                throw new IllegalArgumentException(
                        "lostPackets > " + (1000 / OPUS_FRAME_TIME_MS) * 31 +
                                ": " + Integer.toString(lostPackets)
                );

            for (int i = packetId - lostPackets; i < packetId; i ++) {
                decoder.decodePLC(decoderBuffer);
                receive(i, decoderBuffer);
            }

            decoder.decode(encodedData, decoderBuffer);

            receive(packetId, decoderBuffer);
        } catch (Throwable e) {
            Logger.getGlobal().log(Level.WARNING, "Problem decoding Teamspeak voice", e);
        }
    }

    public Future<?> receiveAsync(int packetId, byte[] data) {
        return executorService.submit(() -> receive(packetId, data));
    }

    public void end() {
        try {
            synchronized (watcherLock) {
                if (ended) return;

                Logger.getGlobal().log(
                        Level.FINE,
                        "Finishing " + client.getNickname() + "'s audio feed..."
                );

                ended = true;

                watcherLock.notifyAll();
            }

            Logger.getGlobal().log(Level.FINE, "Shutting down ExecutorService for " + client.getNickname()
                    + " voice listener...");

            executorService.shutdown(); // fix massive thread leaks

            Logger.getGlobal().log(Level.FINE, "Calling onEnd() for " + client.getNickname()
                    + "'s voice listener...");

            onEnd();

            Logger.getGlobal().log(Level.FINE, "Closing decoder for " + client.getNickname() +
                    "'s voice listener...");

            try {
                decoder.close();
            } catch (Exception e) {
                Logger.getGlobal().log(Level.WARNING, "Problem closing Teamspeak voice listener", e);
            }

            Logger.getGlobal().log(Level.FINE, "Finished listening to " + client.getNickname() + ".");
        } catch (Throwable ex) {
            Logger.getGlobal().log(Level.WARNING, "Problem ending voice listener", ex);
        }
    }

    public Future<?> endAsync() {
        return executorService.submit(this::end);
    }

    @Override
    public void run() {
        try {
            long wait;

            synchronized (watcherLock) {
                while (!ended) {
                    wait = firstPacket ? 250 : 250 - (System.currentTimeMillis() - lastPacket);

                    try {
                        if (wait > 0) watcherLock.wait(Math.max(250, wait));
                    } catch (InterruptedException e) {
                        break;
                    }

                    if (!firstPacket && System.currentTimeMillis() - lastPacket >= 250)
                        break;
                }

                if (!ended) {
                    Logger.getGlobal().log(Level.FINE, "Listener watcher timed out for " + client.getNickname()
                            + "; ending voice listener...");

                    end();
                }
            }
        } catch (Throwable e) {
            Logger.getGlobal().log(Level.WARNING, "Problem in watcher for " + client.getNickname(), e);
        }
    }
}