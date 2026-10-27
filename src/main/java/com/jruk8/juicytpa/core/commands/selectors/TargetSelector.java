package com.jruk8.juicytpa.core.commands.selectors;

import com.jruk8.juicytpa.core.commands.CommandContext;
import com.jruk8.juicytpa.core.messages.MessagesConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Resolves which players a command acts on.
 * <p>
 * An omitted selector means the executor themselves (console gets an error).
 * TPA requests are strictly one-to-one, so an explicit selector must
 * resolve to exactly one player.
 */
public final class TargetSelector {

    private TargetSelector() {
    }

    /**
     * Resolves an omitted selector to the executing player.
     *
     * @param source the command source
     * @param ctx the shared command services
     * @param consoleMessage selects the error sent when the sender is not a player
     * @return the executor alone, or empty when the sender is not a player (error already sent)
     */
    public static Optional<List<Player>> self(
            CommandSourceStack source,
            CommandContext ctx,
            Function<MessagesConfig, String> consoleMessage
    ) {
        if (source.getSender() instanceof Player player) {
            return Optional.of(List.of(player));
        }
        ctx.error(source, consoleMessage, Map.of());
        return Optional.empty();
    }

    /**
     * Resolves an explicit selector to exactly one player.
     *
     * @param source the command source
     * @param selector the parsed selector argument
     * @param ctx the shared command services
     * @return the single resolved player, or empty when resolution fails,
     *         resolves to nobody, or resolves to several players (an error
     *         is already sent in all cases)
     */
    public static Optional<Player> single(
            CommandSourceStack source,
            PlayerSelectorArgumentResolver selector,
            CommandContext ctx
    ) {
        List<Player> targets;
        try {
            targets = selector.resolve(source);
        } catch (CommandSyntaxException unmatched) {
            ctx.error(source, m -> m.getCore().getNoTargets(), Map.of());
            return Optional.empty();
        }
        if (targets.isEmpty()) {
            ctx.error(source, m -> m.getCore().getNoTargets(), Map.of());
            return Optional.empty();
        }
        if (targets.size() > 1) {
            ctx.error(source, m -> m.getTpa().getNotSingle(), Map.of());
            return Optional.empty();
        }
        return Optional.of(targets.getFirst());
    }
}
