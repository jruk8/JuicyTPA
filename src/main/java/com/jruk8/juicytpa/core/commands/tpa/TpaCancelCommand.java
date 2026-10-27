package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.commands.CommandContext;
import com.jruk8.juicytpa.core.commands.TpaPermissions;
import com.jruk8.juicytpa.core.commands.selectors.TargetSelector;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;

import java.util.Optional;

/**
 * The {@code tpacancel} root: bare cancels the single outgoing request, a
 * named receiver picks one of several.
 */
public class TpaCancelCommand {

    private final CommandContext ctx;
    private final TpaService service;

    public TpaCancelCommand(CommandContext ctx, TpaService service) {
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
                .requires(CommandContext.requiresPermission(TpaPermissions.CANCEL))
                .executes(context -> service.cancel(context.getSource(), Optional.empty()))
                .then(Commands.argument("targets", ArgumentTypes.players())
                        .suggests((context, builder) -> {
                            for (String candidate : TpaSuggestions.suggestOutgoing(
                                    service, context.getSource(), builder.getRemainingLowerCase())) {
                                builder.suggest(candidate);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            PlayerSelectorArgumentResolver selector = context.getArgument(
                                    "targets", PlayerSelectorArgumentResolver.class);
                            var target = TargetSelector.single(context.getSource(), selector, ctx);
                            return target.map(player -> service.cancel(context.getSource(), Optional.of(player)))
                                    .orElse(0);
                        }));
    }
}
