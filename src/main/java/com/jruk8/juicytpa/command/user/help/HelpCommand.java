package com.jruk8.juicytpa.command.user.help;

import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.command.Permissions;
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
        HelpEntry.matching(entries(), query).stream()
                .filter(entry -> entry.visibleTo(sender.getSender()))
                .forEach(entry -> ctx.sendMessage(
                        sender,
                        m -> m.getCore().getHelpEntryFormat(),
                        Map.of("syntax", entry.syntax(), "description", entry.description())
                ));
    }

    private List<HelpEntry> entries() {
        return List.of(
                new HelpEntry("/jtpa help [query]", "Shows this help menu."),
                new HelpEntry("/jtpa version", "Shows the plugin version.", Permissions.VERSION),
                new HelpEntry("/tpa <player>", "Asks to teleport to a player.", Permissions.TPA),
                new HelpEntry("/tpahere <player>", "Asks a player to teleport to you.", Permissions.TPAHERE),
                new HelpEntry("/tpaccept [player]", "Accepts a TP request.", Permissions.ACCEPT),
                new HelpEntry("/tpadeny [player]", "Denies a TP request.", Permissions.DENY),
                new HelpEntry("/tpacancel [player]", "Cancels an outgoing TP request.", Permissions.CANCEL),
                new HelpEntry("/tpalist", "Lists your pending TP requests.", Permissions.LIST),
                new HelpEntry(
                        "/tpatrust <player>",
                        "Trusts a player to auto-accept their TP requests.",
                        Permissions.TRUST),
                new HelpEntry("/tpaauto", "Flips between accept-all and normal mode.", Permissions.MODE),
                new HelpEntry(
                        "/tpablockall",
                        "Flips between block-all and normal mode.",
                        Permissions.MODE),
                new HelpEntry("/tpablock <player>", "Blocks or unblocks a player's requests.", Permissions.BLOCK),
                new HelpEntry("/tpall", "Requests everyone at once.", Permissions.TPALL),
                new HelpEntry("/tpamode [mode]", "Cycles or sets your TPA mode.", Permissions.MODE),
                new HelpEntry("/jtpa reload", "Reloads the plugin configuration.", Permissions.RELOAD));
    }
}
