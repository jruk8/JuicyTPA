package com.jruk8.juicytpa.tpa.command;

import com.jruk8.juicytpa.tpa.selector.PlayerSelectorArgument;
import com.jruk8.juicytpa.tpa.selector.TargetSelector;
import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.command.Permissions;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;

import java.util.Map;
import com.jruk8.juicytpa.tpa.TpaService;
import com.jruk8.juicytpa.tpa.TpaSuggestions;

/**
 * The {@code tpatrust} root: toggles the trust between the sender and
 * exactly one player.
 */
public class TpaTrustCommand {

    private final CommandContext ctx;
    private final TpaService service;

    public TpaTrustCommand(CommandContext ctx, TpaService service) {
        this.ctx = ctx;
        this.service = service;
    }

    /**
     * Builds the node for the given label.
     *
     * @param label the root label
     * @return the configured command node
     */
    public LiteralArgumentBuilder<CommandSourceStack> node(String label) {
        return Commands.literal(label)
                .requires(CommandContext.requiresPermission(Permissions.TRUST))
                .executes(context -> {
                    ctx.error(context.getSource(), m -> m.getTpa().getUsage(),
                            Map.of("syntax", "/" + label + " <player>"));
                    return 0;
                })
                .then(Commands.argument("targets", PlayerSelectorArgument.players())
                        .suggests((context, builder) -> {
                            for (String candidate : TpaSuggestions.suggestNames(
                                    context.getSource(), builder.getRemainingLowerCase())) {
                                builder.suggest(candidate);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            PlayerSelectorArgumentResolver selector = context.getArgument(
                                    "targets", PlayerSelectorArgumentResolver.class);
                            var target = TargetSelector.single(context.getSource(), selector, ctx);
                            return target.map(player -> service.trust(context.getSource(), player)).orElse(0);
                        }));
    }
}
