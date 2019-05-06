package io.manebot.plugin.ts3.command;

import io.manebot.chat.TextStyle;
import io.manebot.command.CommandSender;
import io.manebot.command.exception.CommandArgumentException;
import io.manebot.command.exception.CommandExecutionException;
import io.manebot.command.executor.chained.AnnotatedCommandExecutor;
import io.manebot.command.executor.chained.argument.CommandArgumentLabel;
import io.manebot.command.executor.chained.argument.CommandArgumentPage;
import io.manebot.command.executor.chained.argument.CommandArgumentString;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.server.ServerManager;
import io.manebot.user.User;
import io.manebot.user.UserGroup;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.stream.Collectors;

public class TeamspeakCommand extends AnnotatedCommandExecutor {
    private final ServerManager serverManager;

    public TeamspeakCommand(ServerManager serverManager) {
        this.serverManager = serverManager;
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
}
