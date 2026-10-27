package com.jruk8.juicytpa.command.admin.reload;

import com.jruk8.juicytpa.core.Reloader;
import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.command.Permissions;
import com.jruk8.juicytpa.config.PluginConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;
import java.util.Map;

/**
 * Reload subcommand that re-reads every config and re-opens storage and placeholders.
 * <p>
 * Paper registers Brigadier commands once during enable and offers no
 * unregister API, so changed root labels still need a restart; the
 * command says so instead of pretending the labels applied.
 */
public class ReloadCommand {

    private final Reloader reloader;
    private final CommandContext ctx;
    private final PluginConfig pluginConfig;

    public ReloadCommand(Reloader reloader, CommandContext ctx, PluginConfig pluginConfig) {
        this.reloader = reloader;
        this.ctx = ctx;
        this.pluginConfig = pluginConfig;
    }

    /**
     * Builds the {@code reload} Brigadier node, attached to the {@code juicytpa} root by the registrar.
     *
     * @return the configured subcommand node
     */
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return node("reload");
    }

    /**
     * Builds the node for the given label.
     *
     * @param label the root or subcommand label
     * @return the configured command node
     */
    public LiteralArgumentBuilder<CommandSourceStack> node(String label) {
        return Commands.literal(label)
                .requires(CommandContext.requiresPermission(Permissions.RELOAD))
                .executes(context -> {
                    onReload(context.getSource());
                    return Command.SINGLE_SUCCESS;
                });
    }

    public void onReload(CommandSourceStack sender) {
        List<String> aliases = List.copyOf(pluginConfig.getAliases());
        long start = System.currentTimeMillis();
        boolean ok = reloader.reloadAll();
        long time = System.currentTimeMillis() - start;
        if (ok) {
            ctx.sendReload(
                    sender,
                    m -> m.getCore().getReloadSuccess(),
                    Map.of("time", String.valueOf(time))
            );
        } else {
            ctx.error(
                    sender,
                    m -> m.getCore().getReloadFailure(),
                    Map.of("time", String.valueOf(time))
            );
        }
        if (!pluginConfig.getAliases().equals(aliases)) {
            ctx.sendMessage(sender, m -> m.getCore().getReloadRestartRequired(), Map.of());
        }
    }
}
