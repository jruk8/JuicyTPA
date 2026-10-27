package com.jruk8.juicytpa.command;

import com.jruk8.juicytpa.command.admin.AdminCommandsRegistrar;
import com.jruk8.juicytpa.command.user.UserCommandsRegistrar;
import com.jruk8.juicytpa.command.user.help.HelpCommand;
import com.jruk8.juicytpa.config.PluginConfig;
import com.jruk8.juicytpa.core.Bootstrap;
import com.jruk8.juicytpa.core.Reloader;
import com.jruk8.juicytpa.tpa.TpaService;
import com.jruk8.juicytpa.tpa.command.TpaCommandsRegistrar;
import com.jruk8.juicytpa.update.UpdateCheckService;
import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registers all plugin commands through Paper's native Brigadier support.
 * <p>
 * The {@code juicytpa} root (plus its configured aliases) carries every
 * subcommand. Running the root with no arguments shows the help listing.
 */
public class CommandsRegistrar implements Bootstrap {

    private final LifecycleEventManager<Plugin> lifecycle;
    private final Reloader reloader;
    private final CommandContext commandContext;
    private final PluginMeta pluginMeta;
    private final PluginConfig pluginConfig;
    private final UpdateCheckService updateChecks;
    private final TpaService tpaService;
    private final Plugin plugin;

    public CommandsRegistrar(LifecycleEventManager<Plugin> lifecycle,
                             Reloader reloader,
                             CommandContext commandContext,
                             PluginMeta pluginMeta,
                             PluginConfig pluginConfig,
                             UpdateCheckService updateChecks,
                             TpaService tpaService,
                             Plugin plugin) {
        this.lifecycle = lifecycle;
        this.reloader = reloader;
        this.commandContext = commandContext;
        this.pluginMeta = pluginMeta;
        this.pluginConfig = pluginConfig;
        this.updateChecks = updateChecks;
        this.tpaService = tpaService;
        this.plugin = plugin;
    }

    @Override
    public void register() {
        lifecycle.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var root = Commands.literal("juicytpa");
            var hubAliases = rootAliases();
            var help = new HelpCommand(commandContext, pluginMeta.getVersion());
            root.executes(context -> {
                if (!context.getSource().getSender().hasPermission(Permissions.HELP)) {
                    var tags = Map.of("permission", Permissions.HELP);
                    commandContext.error(context.getSource(), m -> m.getCore().getNoPermission(), tags);
                    return 0;
                }
                help.showHelp(context.getSource(), null);
                return Command.SINGLE_SUCCESS;
            });
            new UserCommandsRegistrar(root, commandContext, pluginMeta, updateChecks, help).register();
            new AdminCommandsRegistrar(root, commandContext, reloader, pluginConfig).register();
            var tpa = new TpaCommandsRegistrar(
                    root, commandContext, tpaService, pluginConfig, plugin, plugin.getLogger());
            tpa.register();
            event.registrar().register(root.build(), "JuicyTPA commands", hubAliases);
            tpa.registerStandalone(event.registrar(), hubAliases);
        });
    }

    private List<String> rootAliases() {
        return pluginConfig.getAliases().stream()
                .map(String::trim)
                .map(alias -> alias.toLowerCase(Locale.ROOT))
                .filter(alias -> !alias.isEmpty() && !"juicytpa".equals(alias))
                .distinct()
                .toList();
    }
}
