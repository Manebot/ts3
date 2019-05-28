package io.manebot.plugin.ts3.platform.audio;

import com.github.manevolent.ts3j.identity.Uid;
import io.manebot.conversation.Conversation;
import io.manebot.platform.Platform;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.audio.channel.AudioChannel;
import io.manebot.plugin.audio.channel.AudioChannelRegistrant;
import io.manebot.plugin.audio.mixer.Mixer;

import io.manebot.plugin.ts3.platform.server.TeamspeakServerConnection;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakChannel;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakClient;

import io.manebot.tuple.Pair;
import io.manebot.user.UserAssociation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TeamspeakAudioChannel extends AudioChannel {
    private final TeamspeakServerConnection serverConnection;

    public TeamspeakAudioChannel(Mixer mixer,
                                 AudioChannelRegistrant owner,
                                 TeamspeakServerConnection serverConnection) {
        super(mixer, owner);

        this.serverConnection = serverConnection;
    }

    @Override
    public String getId() {
        return "teamspeak:server:" + serverConnection.getId();
    }

    @Override
    public Platform getPlatform() {
        return serverConnection.getPlatformConnection().getPlatform();
    }

    @Override
    public List<PlatformUser> getMembers() {
        TeamspeakChannel channel = serverConnection.getCurrentChannel();
        if (channel == null) return Collections.emptyList();

        return serverConnection.getRegisteredClients(channel.getClients()).stream()
                .map(Pair::getRight)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<PlatformUser> getListeners() {
        TeamspeakChannel channel = serverConnection.getCurrentChannel();
        if (channel == null) return Collections.emptyList();

        return serverConnection.getRegisteredClients(
                channel.getClients().stream().filter(TeamspeakClient::isListening)
        ).stream()
                .filter(pair -> pair.getLeft().isListening())
                .map(Pair::getRight)
                .collect(Collectors.toList());
    }

    @Override
    public Conversation getConversation() {
        TeamspeakChannel channel = serverConnection.getCurrentChannel();
        if (channel == null) return null;

        return getPlatform().getPlugin().getBot().getConversationProvider()
                .getConversationByChat(serverConnection.getChannelChat(channel));
    }

    @Override
    public Ownership obtainChannel(UserAssociation association) {
        Ownership ownership = this.obtain(association);

        try {
            if (getBlockingPlayers() <= 0 || isIdle()) {
                TeamspeakClient teamspeakClient = serverConnection.findClient(new Uid(association.getPlatformId()));
                if (teamspeakClient == null)
                    throw new IllegalArgumentException("You could not be found in Teamspeak.");

                if (serverConnection.getServer().willFollow())
                    serverConnection.follow(teamspeakClient);
                else {
                    throw new IllegalArgumentException("You are not in the same channel.");
                }
            }

            return ownership;
        } catch (Throwable ex) {
            if (ownership != null) {
                try {
                    ownership.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            throw new RuntimeException(ex);
        }
    }
}
