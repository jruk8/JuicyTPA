package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.commands.CommandContext;
import com.jruk8.juicytpa.core.commands.selectors.TargetSelector;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;

/**
 * A request root ({@code tpa}, {@code tpahere}): sends a teleport request
 * to exactly one player. Plain requests move the sender; {@code here}
 * requests move the receiver.
 */
public class TpaSendCommand {

    private final CommandContext ctx;
    private final TpaService service;
    private final String permission;
    private final boolean here;

    public TpaSendCommand(CommandContext ctx, TpaService service, String permission, boolean here) {
        this.ctx = ctx;
        this.service = service;
        this.permission = permission;
        this.here = here;
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
                .then(Commands.argument("targets", ArgumentTypes.players())
                        .suggests((context, builder) -> {
                            for (String candidate : TpaSuggestions.suggestTargets(
                                    context.getSource(), builder.getRemainingLowerCase())) {
                                builder.suggest(candidate);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            PlayerSelectorArgumentResolver selector = context.getArgument(
                                    "targets", PlayerSelectorArgumentResolver.class);
                            var target = TargetSelector.single(context.getSource(), selector, ctx);
                            return target.map(player -> service.send(context.getSource(), player, here)).orElse(0);
                        }));
    }
}
