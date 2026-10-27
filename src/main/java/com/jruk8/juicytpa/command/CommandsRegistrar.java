package com.jruk8.juicytpa.command;

import com.jruk8.juicytpa.command.admin.AdminCommandsRegistrar;
import com.jruk8.juicytpa.command.user.UserCommandsRegistrar;
import com.jruk8.juicytpa.command.user.help.HelpCommand;
import com.jruk8.juicytpa.config.PluginConfig;
import com.jruk8.juicytpa.core.Bootstrap;
import com.jruk8.juicytpa.core.Reloader;
import com.jruk8.juicytpa.tpa.PlayerLanguage;
import com.jruk8.juicytpa.tpa.TpaService;
import com.jruk8.juicytpa.tpa.command.TpaCommandsRegistrar;
import com.jruk8.juicytpa.update.UpdateCheckService;
import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.ArrayList;
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
    private final PlayerLanguage playerLanguage;
    private final Plugin plugin;

    public CommandsRegistrar(LifecycleEventManager<Plugin> lifecycle,
                             Reloader reloader,
                             CommandContext commandContext,
                             PluginMeta pluginMeta,
                             PluginConfig pluginConfig,
                             UpdateCheckService updateChecks,
                             TpaService tpaService,
                             PlayerLanguage playerLanguage,
                             Plugin plugin) {
        this.lifecycle = lifecycle;
        this.reloader = reloader;
        this.commandContext = commandContext;
        this.pluginMeta = pluginMeta;
        this.pluginConfig = pluginConfig;
        this.updateChecks = updateChecks;
        this.tpaService = tpaService;
        this.playerLanguage = playerLanguage;
        this.plugin = plugin;
    }

    @Override
    public void register() {
        lifecycle.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var hubAliases = rootAliases();
            var help = new HelpCommand(commandContext, pluginMeta.getVersion());
            var mainHub = hub("juicytpa", help);
            event.registrar().register(mainHub.build(), "JuicyTPA commands", hubAliases);
            for (String label : namespacedHubLabels(hubAliases)) {
                event.registrar().register(hub(label, help).build(), "JuicyTPA commands", List.of());
            }
            // registerStandalone never touches the hub builder; any instance works.
            new TpaCommandsRegistrar(
                    mainHub, commandContext, tpaService, playerLanguage,
                    pluginConfig, plugin, plugin.getLogger(),
                    pluginMeta, updateChecks, reloader)
                    .registerStandalone(event.registrar(), hubAliases);
        });
    }

    private LiteralArgumentBuilder<CommandSourceStack> hub(String label, HelpCommand help) {
        var root = Commands.literal(label);
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
        new TpaCommandsRegistrar(
                root, commandContext, tpaService, playerLanguage,
                pluginConfig, plugin, plugin.getLogger(),
                pluginMeta, updateChecks, reloader)
                .register();
        return root;
    }

    private List<String> namespacedHubLabels(List<String> hubAliases) {
        List<String> labels = new ArrayList<>(hubAliases.size() + 1);
        labels.add(TpaCommandsRegistrar.NAMESPACE + ":juicytpa");
        for (String alias : hubAliases) {
            labels.add(TpaCommandsRegistrar.NAMESPACE + ":" + alias);
        }
        return labels;
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
