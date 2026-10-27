package com.jruk8.juicytpa.command.user.help;

import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.command.Permissions;
import com.jruk8.juicytpa.message.MessagesConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;
import java.util.Map;

/**
 * Help subcommand that lists the plugin's commands, optionally filtered by a search query.
 * <p>
 * Senders with {@code juicytpa.help} may run it (everyone by default);
 * entries are filtered by permission. Entries use the main {@code /jtpa}
 * alias.
 */
public class HelpCommand {

    private final CommandContext ctx;
    private final String version;

    public HelpCommand(CommandContext ctx, String version) {
        this.ctx = ctx;
        this.version = version;
    }

    /**
     * Builds the {@code help} Brigadier node, attached to the {@code juicytpa} root by the registrar.
     *
     * @return the configured subcommand node
     */
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("help")
                .requires(CommandContext.requiresPermission(Permissions.HELP))
                .executes(context -> {
                    showHelp(context.getSource(), null);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("query", StringArgumentType.greedyString())
                        .executes(context -> {
                            showHelp(context.getSource(), StringArgumentType.getString(context, "query"));
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    /**
     * Shows the help listing, optionally filtered by a search query.
     *
     * @param sender the sender to show the listing to
     * @param query the search query, may be null or blank
     */
    public void showHelp(CommandSourceStack sender, String query) {
        ctx.playHelp(sender);
        ctx.sendMessage(sender, m -> m.getCore().getHelpHeader(), Map.of("version", version));
        List<HelpEntry> matching =
                HelpEntry.matching(entries(ctx.messagesFor(sender.getSender()).getHelp()), query);
        if (matching.isEmpty()) {
            ctx.sendMessage(sender, m -> m.getCore().getHelpNoMatch(), Map.of());
            return;
        }
        List<HelpEntry> visible = matching.stream()
                .filter(entry -> entry.visibleTo(sender.getSender()))
                .toList();
        if (visible.isEmpty()) {
            ctx.sendMessage(sender, m -> m.getCore().getHelpNoCommands(), Map.of());
            return;
        }
        visible.forEach(entry -> ctx.sendMessage(
                sender,
                m -> m.getCore().getHelpEntryFormat(),
                Map.of("syntax", entry.syntax(), "description", entry.description())
        ));
    }

    private List<HelpEntry> entries(MessagesConfig.Help help) {
        return List.of(
                new HelpEntry("/jtpa help [query]", help.getHelp()),
                new HelpEntry("/jtpa version", help.getVersion(), Permissions.VERSION),
                new HelpEntry("/tpa <player>", help.getTpa(), Permissions.TPA),
                new HelpEntry("/tpahere <player>", help.getTpahere(), Permissions.TPAHERE),
                new HelpEntry("/tpaccept [player]", help.getTpaccept(), Permissions.ACCEPT),
                new HelpEntry("/tpadeny [player]", help.getTpadeny(), Permissions.DENY),
                new HelpEntry("/tpacancel [player]", help.getTpacancel(), Permissions.CANCEL),
                new HelpEntry("/tpalist", help.getTpalist(), Permissions.LIST),
                new HelpEntry("/tpatrust <player>", help.getTpatrust(), Permissions.TRUST),
                new HelpEntry("/tpacceptall", help.getTpacceptall(), Permissions.MODE),
                new HelpEntry("/tpablockall", help.getTpablockall(), Permissions.MODE),
                new HelpEntry("/tpablock <player>", help.getTpablock(), Permissions.BLOCK),
                new HelpEntry("/tpall", help.getTpall(), Permissions.TPALL),
                new HelpEntry("/tpamode [mode]", help.getTpamode(), Permissions.MODE),
                new HelpEntry("/tpalanguage [language]", help.getTpalanguage(), Permissions.LANGUAGE),
                new HelpEntry("/jtpa reload", help.getReload(), Permissions.RELOAD));
    }
}
