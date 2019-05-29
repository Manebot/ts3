package io.manebot.plugin.ts3.command;

import com.github.manevolent.ts3j.command.CommandException;
import io.manebot.chat.TextStyle;
import io.manebot.command.CommandSender;
import io.manebot.command.exception.CommandArgumentException;
import io.manebot.command.exception.CommandExecutionException;
import io.manebot.command.executor.chained.AnnotatedCommandExecutor;
import io.manebot.command.executor.chained.argument.*;
import io.manebot.conversation.Conversation;
import io.manebot.platform.PlatformUser;
import io.manebot.plugin.Plugin;
import io.manebot.plugin.ts3.database.model.TeamspeakServer;
import io.manebot.plugin.ts3.platform.TeamspeakPlatformConnection;
import io.manebot.plugin.ts3.platform.chat.TeamspeakChat;
import io.manebot.plugin.ts3.platform.server.ServerManager;
import io.manebot.plugin.ts3.platform.server.model.TeamspeakClient;
import io.manebot.plugin.ts3.platform.user.TeamspeakPlatformUser;
import io.manebot.tuple.Pair;

import java.io.IOException;
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

    @Command(description = "Joins your channel", permission = "teamspeak.join")
    public void join(CommandSender sender,
                     @CommandArgumentLabel.Argument(label = "join") String join)
            throws CommandExecutionException {
        TeamspeakPlatformUser tsUser;
        TeamspeakChat chat;

        if (sender.getPlatformUser() instanceof TeamspeakPlatformUser) {
            tsUser = (TeamspeakPlatformUser) sender.getPlatformUser();
        } else throw new CommandArgumentException("This command isn't being run from Teamspeak.");

        if (sender.getChat() instanceof TeamspeakChat) {
            chat = (TeamspeakChat) sender.getChat();
        } else throw new CommandArgumentException("This command isn't being run from Teamspeak.");

        TeamspeakClient client = chat.getServer().getConnection().findClient(tsUser.getUid());
        if (client == null || client.getChannel() == null)
            throw new CommandArgumentException("Couldn't find you in Teamspeak. Can the bot subscribe to your channel?");

        try {
            chat.getServer().getConnection().join(client);
        } catch (IOException e) {
            throw new CommandExecutionException("Problem following Teamspeak client", e);
        } catch (CommandException e) {
            throw new CommandExecutionException(e.getMessage());
        }

        sender.sendMessage("(Joining channel)");
    }

    @Command(description = "Lists Teamspeak servers", permission = "teamspeak.identity.show")
    public void showIdentity(CommandSender sender,
                     @CommandArgumentLabel.Argument(label = "identity") String identityLabel,
                     @CommandArgumentLabel.Argument(label = "show") String show)
            throws CommandExecutionException {
        sender.sendDetails(builder -> builder.name("Identity")
                .item("Base64", platformConnection.getIdentity().getUid().toBase64())
                .item("Security Level", Integer.toString(platformConnection.getIdentity().getSecurityLevel()))
        );
    }

    @Command(description = "Lists Teamspeak servers", permission = "teamspeak.server.list")
    public void list(CommandSender sender,
                     @CommandArgumentLabel.Argument(label = "server") String server,
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
                        .responder((textBuilder, s) ->
                                textBuilder.append(s.getId().toLowerCase(), EnumSet.of(TextStyle.BOLD))
                                        .append(s.isConnected() ? " (connected)" : ""))
                        .build()
        );
    }

    @Command(description = "Gets Teamspeak server information", permission = "teamspeak.server.info")
    public void info(CommandSender sender,
                     @CommandArgumentLabel.Argument(label = "server") String serverLabel,
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
                .item("Follow Clients", server.willFollow())
                .item("Idle Timeout (s)", Integer.toString(server.getIdleTimeout()))
        );
    }

    @Command(description = "Adds a Teamspeak server to the list", permission = "teamspeak.server.add")
    public void add(CommandSender sender,
                    @CommandArgumentLabel.Argument(label = "server") String serverLabel,
                    @CommandArgumentLabel.Argument(label = "add") String add,
                    @CommandArgumentString.Argument(label = "id") String id,
                    @CommandArgumentString.Argument(label = "endpoint") String endpoint)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        TeamspeakServer server = serverManager.getServer(id);
        if (server != null) throw new CommandArgumentException("Server \"" + id + "\" already exists.");

        server = serverManager.addServer(id, endpoint);

        sender.sendMessage("Server \"" + server.getId() + "\" created.");
    }

    @Command(description = "Adds a Teamspeak server to the list with a password", permission = "teamspeak.server.add")
    public void add(CommandSender sender,
                    @CommandArgumentLabel.Argument(label = "server") String serverLabel,
                    @CommandArgumentLabel.Argument(label = "add") String add,
                    @CommandArgumentString.Argument(label = "id") String id,
                    @CommandArgumentString.Argument(label = "endpoint") String endpoint,
                    @CommandArgumentFollowing.Argument() String password)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        TeamspeakServer server = serverManager.getServer(id);
        if (server != null) throw new CommandArgumentException("Server \"" + id + "\" already exists.");

        server = serverManager.addServer(id, endpoint);
        server.setPassword(password);

        sender.sendMessage("Server \"" + server.getId() + "\" created.");
    }

    @Command(description = "Removes a Teamspeak server from the list", permission = "teamspeak.server.remove")
    public void remove(CommandSender sender,
                       @CommandArgumentLabel.Argument(label = "server") String serverLabel,
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
                       @CommandArgumentLabel.Argument(label = "server") String serverLabel,
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
                        @CommandArgumentLabel.Argument(label = "server") String serverLabel,
                        @CommandArgumentLabel.Argument(label = "disable") String disable,
                        @CommandArgumentString.Argument(label = "id") String id)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        TeamspeakServer server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Server does not exist.");

        if (!server.isEnabled()) throw new CommandArgumentException("Server is already disabled.");

        server.setEnabled(false);
        sender.sendMessage("Server \"" + server.getId() + "\" disabled.");
    }

    @Command(description = "Sets a server property", permission = "teamspeak.server.property.set")
    public void set(CommandSender sender,
                    @CommandArgumentLabel.Argument(label = "server") String serverLabel,
                    @CommandArgumentLabel.Argument(label = "set") String enable,
                    @CommandArgumentString.Argument(label = "id") String id,
                    @CommandArgumentSwitch.Argument(labels =
                            {"away-channel","lobby-channel","password","nickname","idle-timeout","follow"}) String propertyKey,
                    @CommandArgumentFollowing.Argument() String value)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        TeamspeakServer server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Server does not exist.");

        TeamspeakConnectionProperty property = TeamspeakConnectionProperty.fromName(propertyKey);
        property.getSetter().accept(new Pair<>(server, value));

        sender.sendMessage("Server \"" + server.getId() + "\" property \"" +
                property.getName() + "\" set to \"" + value + "\".");
    }

    @Command(description = "Unsets a server property", permission = "teamspeak.server.property.unset")
    public void unset(CommandSender sender,
                      @CommandArgumentLabel.Argument(label = "server") String serverLabel,
                      @CommandArgumentLabel.Argument(label = "unset") String disable,
                      @CommandArgumentString.Argument(label = "id") String id,
                      @CommandArgumentSwitch.Argument(labels =
                              {"away-channel","lobby-channel","password","nickname","idle-timeout","follow"}) String propertyKey)
            throws CommandExecutionException {
        id = id.toLowerCase().trim();

        TeamspeakServer server = serverManager.getServer(id);
        if (server == null) throw new CommandArgumentException("Server does not exist.");

        TeamspeakConnectionProperty property = TeamspeakConnectionProperty.valueOf(propertyKey);
        property.getSetter().accept(new Pair<>(server, null));

        sender.sendMessage("Server \"" + server.getId() + "\" property \"" + property.getName() + "\" unset.");
    }
}
