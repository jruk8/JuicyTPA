package com.jruk8.juicytpa.core.commands.selectors;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * The vanilla players() argument that also captures the raw input text,
 * so resolution can special-case selectors like {@code @r}. Wrapped as a
 * custom argument type with the players() native type for the client.
 */
public final class PlayerSelectorArgument
        implements CustomArgumentType<PlayerSelectorArgumentResolver, PlayerSelectorArgumentResolver> {

    /**
     * @return a fresh players() argument with raw-input capture
     */
    public static PlayerSelectorArgument players() {
        return new PlayerSelectorArgument();
    }

    @Override
    public PlayerSelectorArgumentResolver parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        PlayerSelectorArgumentResolver resolver = ArgumentTypes.players().parse(reader);
        return new Captured(resolver, reader.getString().substring(start, reader.getCursor()));
    }

    @Override
    public ArgumentType<PlayerSelectorArgumentResolver> getNativeType() {
        return ArgumentTypes.players();
    }

    /**
     * A parsed selector plus the raw text it was parsed from.
     *
     * @param delegate the vanilla resolver doing the actual work
     * @param rawInput the raw selector text
     */
    public record Captured(PlayerSelectorArgumentResolver delegate, String rawInput)
            implements PlayerSelectorArgumentResolver {

        @Override
        public List<Player> resolve(CommandSourceStack source) throws CommandSyntaxException {
            return delegate.resolve(source);
        }
    }
}
