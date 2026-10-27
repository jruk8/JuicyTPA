package com.jruk8.juicytpa.tpa.command;

import com.jruk8.juicytpa.tpa.selector.PlayerSelectorArgument;
import com.jruk8.juicytpa.tpa.selector.TargetSelector;
import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.command.Permissions;
import com.jruk8.juicytpa.command.user.help.HelpCommand;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import com.jruk8.juicytpa.tpa.TpaService;
import com.jruk8.juicytpa.tpa.TpaSuggestions;

/**
 * A request root ({@code tpa}, {@code tpahere}): sends a teleport request
 * to one player, or to several at once for senders holding the tpall
 * permission. Plain requests move the sender; {@code here} requests move
 * the receiver.
 */
public class TpaSendCommand {

    private final CommandContext ctx;
    private final TpaService service;
    private final String permission;
    private final boolean here;
    private final HelpCommand help;

    public TpaSendCommand(
            CommandContext ctx, TpaService service, String permission, boolean here, HelpCommand help) {
        this.ctx = ctx;
        this.service = service;
        this.permission = permission;
        this.here = here;
        this.help = help;
    }

    /**
     * Builds the node for the given label.
     *
     * @param label the root label
     * @return the configured command node
     */
    public LiteralArgumentBuilder<CommandSourceStack> node(String label) {
        return Commands.literal(label)
                .requires(CommandContext.requiresPermission(permission))
                .executes(context -> {
                    ctx.error(context.getSource(), m -> m.getTpa().getUsage(),
                            Map.of("syntax", "/" + label + " <player>"));
                    return 0;
                })
                .then(Commands.argument("targets", PlayerSelectorArgument.players())
                        .suggests((context, builder) -> {
                            // Best effort first: clients may re-sort, but ours goes out first.
                            if (context.getSource().getSender().hasPermission(Permissions.HELP)
                                    && "help".startsWith(builder.getRemainingLowerCase())) {
                                builder.suggest("help");
                            }
                            for (String candidate : TpaSuggestions.suggestTargets(
                                    context.getSource(), builder.getRemainingLowerCase())) {
                                builder.suggest(candidate);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> execute(
                                context.getSource(),
                                context.getArgument("targets", PlayerSelectorArgumentResolver.class))));
    }

    /**
     * Runs a parsed send: the help listing on the help selector, else
     * a request to the resolved targets. Package-visible for testing;
     * production calls it through the node's executes handler.
     *
     * @param source the command source
     * @param selector the parsed target selector
     * @return the Brigadier result code
     */
    int execute(CommandSourceStack source, PlayerSelectorArgumentResolver selector) {
        if (isHelpRequest(selector)) {
            return showHelp(source);
        }
        boolean multi = source.getSender().hasPermission(Permissions.TPALL);
        var targets = TargetSelector.group(source, selector, multi, ctx);
        return targets.map(players -> send(source, players)).orElse(0);
    }

    private int send(CommandSourceStack source, List<Player> targets) {
        if (targets.size() > 1) {
            return service.sendBulk(source, targets, here);
        }
        return service.send(source, targets.getFirst(), here);
    }

    private static boolean isHelpRequest(PlayerSelectorArgumentResolver selector) {
        return selector instanceof PlayerSelectorArgument.Captured captured
                && captured.rawInput().trim().equalsIgnoreCase("help")
                && noPlayerNamedHelp();
    }

    private static boolean noPlayerNamedHelp() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase("help")) {
                return false;
            }
        }
        return true;
    }

    private int showHelp(CommandSourceStack source) {
        if (!source.getSender().hasPermission(Permissions.HELP)) {
            ctx.error(source, m -> m.getCore().getNoPermission(), Map.of("permission", Permissions.HELP));
            return 0;
        }
        help.showHelp(source, null);
        return Command.SINGLE_SUCCESS;
    }
}
