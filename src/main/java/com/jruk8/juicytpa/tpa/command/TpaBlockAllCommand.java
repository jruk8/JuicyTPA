package com.jruk8.juicytpa.tpa.command;

import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.command.Permissions;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.jruk8.juicytpa.tpa.TpaService;

/**
 * The {@code tpablockall} root: flips the sender between block-all
 * mode and normal mode with one command.
 */
public class TpaBlockAllCommand {

    private final TpaService service;

    public TpaBlockAllCommand(TpaService service) {
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
                .executes(context -> service.blockAllMode(context.getSource()));
    }
}
