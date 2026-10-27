package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.commands.CommandContext;
import com.jruk8.juicytpa.core.commands.TpaPermissions;
import com.jruk8.juicytpa.core.commands.selectors.PlayerSelectorArgument;
import com.jruk8.juicytpa.core.commands.selectors.TargetSelector;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;

/**
 * The {@code tpablock} root: toggles the block between the sender and
 * exactly one player.
 */
public class TpaBlockCommand {

    private final CommandContext ctx;
    private final TpaService service;

    public TpaBlockCommand(CommandContext ctx, TpaService service) {
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
                .requires(CommandContext.requiresPermission(TpaPermissions.BLOCK))
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
                            return target.map(player -> service.block(context.getSource(), player)).orElse(0);
                        }));
    }
}
