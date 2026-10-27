package com.jruk8.juicytpa.tpa.command;

import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.command.Permissions;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.jruk8.juicytpa.tpa.TpaService;

/**
 * The {@code tpalist} root: lists the sender's pending incoming and
 * outgoing requests grouped by direction and request type.
 */
public class TpaListCommand {

    private final CommandContext ctx;
    private final TpaService service;

    public TpaListCommand(CommandContext ctx, TpaService service) {
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
                .requires(CommandContext.requiresPermission(Permissions.LIST))
                .executes(context -> service.list(context.getSource()));
    }
}
