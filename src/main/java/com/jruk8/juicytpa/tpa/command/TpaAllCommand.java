package com.jruk8.juicytpa.tpa.command;

import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.command.Permissions;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import com.jruk8.juicytpa.tpa.TpaService;

/**
 * The {@code tpall} root: sends a plain request to every online player.
 * Bypasses concurrency entirely; duplicates, blocked pairs and
 * block-all receivers are skipped silently.
 */
public class TpaAllCommand {

    private final CommandContext ctx;
    private final TpaService service;

    public TpaAllCommand(CommandContext ctx, TpaService service) {
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
                .requires(CommandContext.requiresPermission(Permissions.TPALL))
                .executes(context -> {
                    List<Player> targets = new ArrayList<>(Bukkit.getOnlinePlayers());
                    return service.sendBulk(context.getSource(), targets, false);
                });
    }
}
