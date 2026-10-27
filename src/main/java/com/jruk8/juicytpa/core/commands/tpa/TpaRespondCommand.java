package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.commands.CommandContext;
import com.jruk8.juicytpa.core.commands.selectors.PlayerSelectorArgument;
import com.jruk8.juicytpa.core.commands.selectors.TargetSelector;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * An answer root ({@code tpaccept}, {@code tpadeny}): bare answers the
 * single pending request, a named sender picks one of several.
 */
public class TpaRespondCommand {

    private final CommandContext ctx;
    private final TpaService service;
    private final String permission;
    private final boolean accept;

    public TpaRespondCommand(CommandContext ctx, TpaService service, String permission, boolean accept) {
        this.ctx = ctx;
        this.service = service;
        this.permission = permission;
        this.accept = accept;
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
                .executes(context -> answer(context.getSource(), Optional.empty()))
                .then(Commands.argument("targets", PlayerSelectorArgument.players())
                        .suggests((context, builder) -> {
                            for (String candidate : TpaSuggestions.suggestIncoming(
                                    service, context.getSource(), builder.getRemainingLowerCase())) {
                                builder.suggest(candidate);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            PlayerSelectorArgumentResolver selector = context.getArgument(
                                    "targets", PlayerSelectorArgumentResolver.class);
                            var sender = TargetSelector.single(context.getSource(), selector, ctx);
                            return sender.map(player -> answer(context.getSource(), Optional.of(player))).orElse(0);
                        }));
    }

    private int answer(CommandSourceStack source, Optional<Player> named) {
        return accept ? service.accept(source, named) : service.deny(source, named);
    }
}
