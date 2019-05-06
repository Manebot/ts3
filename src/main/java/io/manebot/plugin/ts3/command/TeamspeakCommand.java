package io.manebot.plugin.ts3.command;

import io.manebot.chat.TextStyle;
import io.manebot.command.CommandSender;
import io.manebot.command.exception.CommandArgumentException;
import io.manebot.command.exception.CommandExecutionException;
import io.manebot.command.executor.chained.AnnotatedCommandExecutor;
import io.manebot.command.executor.chained.argument.CommandArgumentLabel;
import io.manebot.command.executor.chained.argument.CommandArgumentPage;
import io.manebot.command.executor.chained.argument.CommandArgumentString;
import io.manebot.plugin.Plugin;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;
import io.manebot.plugin.ts3.platform.server.ServerManager;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.stream.Collectors;

public class TeamspeakCommand extends AnnotatedCommandExecutor {
    private final ServerManager serverManager;
    private final TeamspeakPlatformConnection platformConnection;

    public TeamspeakCommand(Plugin plugin, ServerManager serverManager) {
        this.serverManager = serverManager;
        this.platformConnection = (TeamspeakPlatformConnection) plugin.getPlatformById("ts3").getConnection();
    }

    @Command(description = "Lists Teamspeak servers", permission = "teamspeak.server.list")
    public void list(CommandSender sender,
                     @CommandArgumentLabel.Argument(label = "list") String list,
                     @CommandArgumentPage.Argument int page)
            throws CommandExecutionException {
        sender.sendList(
                TeamspeakServer.class,
                builder -> builder.direct(
                        serverManager.getServers()
                                .stream()
                                .sorted(Comparator.comparing(TeamspeakServer::getId))
                                .collect(Collectors.toList()))
                        .page(page)
                        .responder((textBuilder, server) ->
                                textBuilder.append(server.getId().toLowerCase(), EnumSet.of(TextStyle.BOLD))
                                        .append(server.isConnected() ? " (connected)" : ""))
                        .build()
        );
    }

    @Command(description = "Gets Teamspeak server information", permission = "teamspeak.server.info")
    public void info(CommandSender sender,
                     @CommandArgumentLabel.Argument(label = "info") String info,
                     @CommandArgumentString.Argument(label = "id") String id)
            throws CommandExecutionException {
        TeamspeakServer server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Server not found.");

        sender.sendDetails(builder -> builder.name("Server").key(server.getId().toLowerCase())
                .item("Enabled", Boolean.toString(server.isEnabled()))
                .item("Connected", Boolean.toString(server.isConnected()))
                .item("Address", server.getEndpoint())
                .item("Display Name", server.getDisplayName())
                .item("Away Channel", server.getAwayChannel())
                .item("Lobby Channel", server.getLobbyChannel())
                .item("Idle Timeout (s)", Integer.toString(server.getIdleTimeout()))
        );
    }

    @Command(description = "Adds a Teamspeak server to the list", permission = "teamspeak.server.add")
    public void add(CommandSender sender,
                     @CommandArgumentLabel.Argument(label = "add") String add,
                     @CommandArgumentString.Argument(label = "id") String id,
                     @CommandArgumentString.Argument(label = "endpoint") String endpoint)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        TeamspeakServer server = serverManager.getServer(id);
        if (server != null) throw new CommandArgumentException("Server \"" + id + "\" already exists.");

        server = serverManager.createServer(id);
        sender.sendMessage("Server \"" + server.getId() + "\" created.");
    }

    @Command(description = "Removes a Teamspeak server from the list", permission = "teamspeak.server.remove")
    public void remove(CommandSender sender,
                    @CommandArgumentLabel.Argument(label = "remove") String remove,
                    @CommandArgumentString.Argument(label = "id") String id)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        TeamspeakServer server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Server does not exist.");

        server.setEnabled(false);

        server = serverManager.removeServer(id);

        sender.sendMessage("Server \"" + server.getId() + "\" removed.");
    }

    @Command(description = "Enables a server", permission = "teamspeak.server.enable")
    public void enable(CommandSender sender,
                    @CommandArgumentLabel.Argument(label = "enable") String enable,
                    @CommandArgumentString.Argument(label = "id") String id)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        TeamspeakServer server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Server does not exist.");

        if (server.isEnabled()) throw new CommandArgumentException("Server is already enabled.");

        server.setEnabled(true);

        platformConnection.connectToServer(server);

        sender.sendMessage("Server \"" + server.getId() + "\" enabled.");
    }

    @Command(description = "Disables a server", permission = "teamspeak.server.disable")
    public void disable(CommandSender sender,
                       @CommandArgumentLabel.Argument(label = "enable") String disable,
                       @CommandArgumentString.Argument(label = "id") String id)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        TeamspeakServer server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Server does not exist.");

        if (!server.isEnabled()) throw new CommandArgumentException("Server is already disabled.");

        server.setEnabled(false);
        sender.sendMessage("Server \"" + server.getId() + "\" disabled.");
    }
}
