package com.jruk8.juicytpa.tpa.command;

import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.command.Permissions;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.Optional;
import com.jruk8.juicytpa.tpa.TpaService;
import com.jruk8.juicytpa.tpa.TpaSuggestions;

/**
 * The {@code tpamode} root: bare cycles the sender's mode, a mode name
 * sets it directly.
 */
public class TpaModeCommand {

    private final CommandContext ctx;
    private final TpaService service;

    public TpaModeCommand(CommandContext ctx, TpaService service) {
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
                .requires(CommandContext.requiresPermission(Permissions.MODE))
                .executes(context -> service.mode(context.getSource(), Optional.empty()))
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (String candidate : TpaSuggestions.suggestModes(builder.getRemainingLowerCase())) {
                                builder.suggest(candidate);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String input = StringArgumentType.getString(context, "mode");
                            return service.mode(context.getSource(), Optional.of(input));
                        }));
    }
}
