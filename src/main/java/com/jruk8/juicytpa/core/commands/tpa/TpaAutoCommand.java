package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.commands.CommandContext;
import com.jruk8.juicytpa.core.commands.TpaPermissions;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * The {@code tpaauto} root: flips the sender between accept-all mode
 * and normal mode with one command.
 */
public class TpaAutoCommand {

    private final TpaService service;

    public TpaAutoCommand(TpaService service) {
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
                .requires(CommandContext.requiresPermission(TpaPermissions.MODE))
                .executes(context -> service.autoMode(context.getSource()));
    }
}
