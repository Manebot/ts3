package io.manebot.plugin.ts3;

import io.manebot.artifact.ManifestIdentifier;
import io.manebot.database.Database;
import io.manebot.plugin.Plugin;
import io.manebot.plugin.PluginException;
import io.manebot.plugin.PluginType;
import io.manebot.plugin.audio.Audio;
import io.manebot.plugin.audio.api.AudioRegistration;
import io.manebot.plugin.java.PluginEntry;
import io.manebot.plugin.ts3.command.TeamspeakCommand;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.server.ServerManager;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;
import io.manebot.virtual.Virtual;

import java.util.Arrays;
import java.util.logging.Level;

public class Entry implements PluginEntry {
    @Override
    public void instantiate(Plugin.Builder builder) throws PluginException {
        builder.setType(PluginType.FEATURE);

        final Database database = builder.addDatabase("teamspeak", modelConstructor -> {
            modelConstructor.addDependency(modelConstructor.getSystemDatabase());
            modelConstructor.registerEntity(TeamspeakServer.class);
        });

        builder.setInstance(ServerManager.class, plugin -> new ServerManager(database));

        builder.addCommand(Arrays.asList("teamspeak", "ts"), new TeamspeakCommand());

        builder.addPlatform(platformBuilder -> {
            platformBuilder.setId("ts3").setName("Teamspeak3");

            Plugin audioPlugin;
            try {
                audioPlugin = builder.getPlugin(ManifestIdentifier.fromString("io.manebot.plugin:audio"));
            } catch (Throwable e) {
                Virtual.getInstance().getLogger().log(Level.WARNING, "Failed to require audio plugin", e);
                audioPlugin = null;
            }

            Audio audio = audioPlugin != null ? audioPlugin.getInstance(Audio.class) : null;

            final TeamspeakPlatformConnection platformConnection = new TeamspeakPlatformConnection(
                    platformBuilder.getPlatform(),
                    audio
            );

            AudioRegistration registration;
            if (audio != null)
                registration = audio.createRegistration(
                        platformBuilder.getPlatform(),
                        consumer -> consumer.setConnection(platformConnection.getAudioConnection())
                );
            else
                registration = null;

            platformBuilder.setConnection(platformConnection);
        });
    }
}
