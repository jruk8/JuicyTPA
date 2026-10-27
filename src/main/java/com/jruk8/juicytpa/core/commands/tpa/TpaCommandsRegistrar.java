package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.Bootstrap;
import com.jruk8.juicytpa.core.commands.CommandContext;
import com.jruk8.juicytpa.core.commands.TpaPermissions;
import com.jruk8.juicytpa.core.configs.PluginConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Registers the TPA subcommands on the hub root and the standalone roots.
 * Root labels are only registered when free; taken labels are skipped with
 * a console warning so one contested alias can never break the plugin.
 */
public class TpaCommandsRegistrar implements Bootstrap {

    private static final Set<String> HUB_COMMANDS = Set.of(
            "tpa", "tpahere", "tpaccept", "tpadeny", "tpacancel", "tpamode", "tpablock", "tpall", "tpalist",
            "tpatrust", "tpaauto", "tpablockall", "reload", "help", "version");

    private final LiteralArgumentBuilder<CommandSourceStack> root;
    private final CommandContext ctx;
    private final TpaService service;
    private final PluginConfig pluginConfig;
    private final Plugin plugin;
    private final Logger logger;

    public TpaCommandsRegistrar(
            LiteralArgumentBuilder<CommandSourceStack> root,
            CommandContext commandContext,
            TpaService service,
            PluginConfig pluginConfig,
            Plugin plugin,
            Logger logger
    ) {
        this.root = root;
        this.ctx = commandContext;
        this.service = service;
        this.pluginConfig = pluginConfig;
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public void register() {
        var used = new HashSet<>(HUB_COMMANDS);
        registerSend("tpa", false, channel().getTpa().getAliases(), TpaPermissions.TPA, used);
        registerSend("tpahere", true, channel().getTpahere().getAliases(), TpaPermissions.TPAHERE, used);
        registerRespond("tpaccept", true, channel().getTpaccept().getAliases(), TpaPermissions.ACCEPT, used);
        registerRespond("tpadeny", false, channel().getTpadeny().getAliases(), TpaPermissions.DENY, used);
        hubSubcommand("tpacancel", channel().getTpacancel().getAliases(),
                new TpaCancelCommand(ctx, service)::node, used);
        hubSubcommand("tpamode", channel().getTpamode().getAliases(),
                new TpaModeCommand(ctx, service)::node, used);
        hubSubcommand("tpablock", channel().getTpablock().getAliases(),
                new TpaBlockCommand(ctx, service)::node, used);
        hubSubcommand("tpall", channel().getTpall().getAliases(),
                new TpaAllCommand(ctx, service)::node, used);
        hubSubcommand("tpalist", channel().getTpalist().getAliases(),
                new TpaListCommand(ctx, service)::node, used);
        hubSubcommand("tpatrust", channel().getTpatrust().getAliases(),
                new TpaTrustCommand(ctx, service)::node, used);
        hubSubcommand("tpaauto", channel().getTpaauto().getAliases(),
                new TpaAutoCommand(service)::node, used);
        hubSubcommand("tpablockall", channel().getTpablockall().getAliases(),
                new TpaBlockAllCommand(service)::node, used);
    }

    private void hubSubcommand(
            String label,
            List<String> aliases,
            Function<String, LiteralArgumentBuilder<CommandSourceStack>> node,
            Set<String> used) {
        root.then(node.apply(label));
        for (String alias : hubAliases(aliases, used)) {
            root.then(node.apply(alias));
        }
    }

    /**
     * Registers every standalone TPA root.
     *
     * @param registrar the Paper commands registrar
     * @param hubLabels labels already taken by the hub root and its aliases
     */
    public void registerStandalone(Commands registrar, List<String> hubLabels) {
        var taken = new HashSet<>(hubLabels);
        taken.addAll(HUB_COMMANDS);
        registerSendRoot(registrar, "tpa", false, channel().getTpa(), TpaPermissions.TPA, taken);
        registerSendRoot(registrar, "tpahere", true, channel().getTpahere(), TpaPermissions.TPAHERE, taken);
        registerRespondRoot(registrar, "tpaccept", true, channel().getTpaccept(), TpaPermissions.ACCEPT, taken);
        registerRespondRoot(registrar, "tpadeny", false, channel().getTpadeny(), TpaPermissions.DENY, taken);
        registerCancelRoot(registrar, channel().getTpacancel(), taken);
        registerModeRoot(registrar, channel().getTpamode(), taken);
        registerBlockRoot(registrar, channel().getTpablock(), taken);
        registerAllRoot(registrar, channel().getTpall(), taken);
        registerListRoot(registrar, channel().getTpalist(), taken);
        registerTrustRoot(registrar, channel().getTpatrust(), taken);
        registerAutoRoot(registrar, channel().getTpaauto(), taken);
        registerBlockAllRoot(registrar, channel().getTpablockall(), taken);
    }

    private void registerSend(String label, boolean here, List<String> aliases, String permission, Set<String> used) {
        var command = new TpaSendCommand(ctx, service, permission, here);
        root.then(command.node(label));
        for (String alias : hubAliases(aliases, used)) {
            root.then(command.node(alias));
        }
    }

    private void registerRespond(
            String label, boolean accept, List<String> aliases, String permission, Set<String> used) {
        var command = new TpaRespondCommand(ctx, service, permission, accept);
        root.then(command.node(label));
        for (String alias : hubAliases(aliases, used)) {
            root.then(command.node(alias));
        }
    }

    private void registerSendRoot(Commands registrar,
                                  String label,
                                  boolean here,
                                  PluginConfig.Channel channel,
                                  String permission,
                                  Set<String> taken) {
        if (!channel.isEnabled()) {
            return;
        }
        registerRoot(registrar, new TpaSendCommand(ctx, service, permission, here).node(label).build(),
                "JuicyTPA " + label + " command", channelAliases(channel.getAliases(), taken));
    }

    private void registerRespondRoot(Commands registrar,
                                     String label,
                                     boolean accept,
                                     PluginConfig.Channel channel,
                                     String permission,
                                     Set<String> taken) {
        if (!channel.isEnabled()) {
            return;
        }
        registerRoot(registrar, new TpaRespondCommand(ctx, service, permission, accept).node(label).build(),
                "JuicyTPA " + label + " command", channelAliases(channel.getAliases(), taken));
    }

    private void registerCancelRoot(Commands registrar, PluginConfig.Channel channel, Set<String> taken) {
        if (!channel.isEnabled()) {
            return;
        }
        registerRoot(registrar, new TpaCancelCommand(ctx, service).node("tpacancel").build(),
                "JuicyTPA tpacancel command", channelAliases(channel.getAliases(), taken));
    }

    private void registerModeRoot(Commands registrar, PluginConfig.Channel channel, Set<String> taken) {
        if (!channel.isEnabled()) {
            return;
        }
        registerRoot(registrar, new TpaModeCommand(ctx, service).node("tpamode").build(),
                "JuicyTPA tpamode command", channelAliases(channel.getAliases(), taken));
    }

    private void registerBlockRoot(Commands registrar, PluginConfig.Channel channel, Set<String> taken) {
        if (!channel.isEnabled()) {
            return;
        }
        registerRoot(registrar, new TpaBlockCommand(ctx, service).node("tpablock").build(),
                "JuicyTPA tpablock command", channelAliases(channel.getAliases(), taken));
    }

    private void registerAllRoot(Commands registrar, PluginConfig.Channel channel, Set<String> taken) {
        if (!channel.isEnabled()) {
            return;
        }
        registerRoot(registrar, new TpaAllCommand(ctx, service).node("tpall").build(),
                "JuicyTPA tpall command", channelAliases(channel.getAliases(), taken));
    }

    private void registerListRoot(Commands registrar, PluginConfig.Channel channel, Set<String> taken) {
        if (!channel.isEnabled()) {
            return;
        }
        registerRoot(registrar, new TpaListCommand(ctx, service).node("tpalist").build(),
                "JuicyTPA tpalist command", channelAliases(channel.getAliases(), taken));
    }

    private void registerTrustRoot(Commands registrar, PluginConfig.Channel channel, Set<String> taken) {
        if (!channel.isEnabled()) {
            return;
        }
        registerRoot(registrar, new TpaTrustCommand(ctx, service).node("tpatrust").build(),
                "JuicyTPA tpatrust command", channelAliases(channel.getAliases(), taken));
    }

    private void registerAutoRoot(Commands registrar, PluginConfig.Channel channel, Set<String> taken) {
        if (!channel.isEnabled()) {
            return;
        }
        registerRoot(registrar, new TpaAutoCommand(service).node("tpaauto").build(),
                "JuicyTPA tpaauto command", channelAliases(channel.getAliases(), taken));
    }

    private void registerBlockAllRoot(Commands registrar, PluginConfig.Channel channel, Set<String> taken) {
        if (!channel.isEnabled()) {
            return;
        }
        registerRoot(registrar, new TpaBlockAllCommand(service).node("tpablockall").build(),
                "JuicyTPA tpablockall command", channelAliases(channel.getAliases(), taken));
    }

    private void registerRoot(
            Commands registrar,
            com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> node,
            String description,
            List<String> aliases
    ) {
        if (!claimLabel(node.getLiteral())) {
            return;
        }
        List<String> free = new ArrayList<>();
        for (String alias : aliases) {
            if (claimLabel(alias)) {
                free.add(alias);
            }
        }
        registrar.register(node, description, free);
    }

    /**
     * Checks a label against the server command map. Taken labels are
     * skipped with a warning.
     *
     * @param label the label to claim
     * @return {@code true} when the label is free
     */
    private boolean claimLabel(String label) {
        Command existing = Bukkit.getCommandMap().getCommand(label);
        if (existing == null) {
            return true;
        }
        if (existing instanceof PluginCommand pluginCommand && pluginCommand.getPlugin().equals(plugin)) {
            return true;
        }
        logger.warning("Command label '/" + label + "' is already registered by "
                + describe(existing) + "; skipping JuicyTPA's '/" + label + "' root.");
        return false;
    }

    private String describe(Command existing) {
        if (existing instanceof PluginCommand pluginCommand) {
            return "the plugin '" + pluginCommand.getPlugin().getName() + "'";
        }
        return "another command source";
    }

    private PluginConfig.Commands channel() {
        return pluginConfig.getCommands();
    }

    private List<String> hubAliases(List<String> configured, Set<String> used) {
        List<String> accepted = new ArrayList<>();
        for (String raw : configured) {
            String alias = raw.trim().toLowerCase(Locale.ROOT);
            if (!alias.isEmpty() && used.add(alias)) {
                accepted.add(alias);
            }
        }
        return accepted;
    }

    private List<String> channelAliases(List<String> configured, Set<String> taken) {
        List<String> accepted = new ArrayList<>();
        for (String raw : configured) {
            String alias = raw.trim().toLowerCase(Locale.ROOT);
            if (!alias.isEmpty() && taken.add(alias)) {
                accepted.add(alias);
            }
        }
        return accepted;
    }
}
