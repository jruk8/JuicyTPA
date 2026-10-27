package com.jruk8.juicytpa.command.user.version;

import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.command.Permissions;
import com.jruk8.juicytpa.update.UpdateAvailable;
import com.jruk8.juicytpa.update.UpdateCheckService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.configuration.PluginMeta;

import java.util.List;
import java.util.Map;

/**
 * Version subcommand that displays the plugin version and authors.
 */
public class VersionCommand {

    private final AuthorFormatter authorFormatter;
    private final CommandContext ctx;
    private final PluginMeta pluginMeta;
    private final UpdateCheckService updateChecks;

    public VersionCommand(
            AuthorFormatter authorFormatter,
            CommandContext ctx,
            PluginMeta pluginMeta,
            UpdateCheckService updateChecks
    ) {
        this.authorFormatter = authorFormatter;
        this.ctx = ctx;
        this.pluginMeta = pluginMeta;
        this.updateChecks = updateChecks;
    }

    /**
     * Builds the {@code version} Brigadier node, attached to the {@code juicytpa} root by the registrar.
     *
     * @return the configured subcommand node
     */
    public LiteralArgumentBuilder<CommandSourceStack> node() {
        return node("version");
    }

    /**
     * Builds the node for the given label.
     *
     * @param label the root or subcommand label
     * @return the configured command node
     */
    public LiteralArgumentBuilder<CommandSourceStack> node(String label) {
        return Commands.literal(label)
                .requires(CommandContext.requiresPermission(Permissions.VERSION))
                .executes(context -> {
                    onVersion(context.getSource());
                    return Command.SINGLE_SUCCESS;
                });
    }

    public void onVersion(CommandSourceStack sender) {
        String version = pluginMeta.getVersion();
        List<String> rawAuthors = pluginMeta.getAuthors();
        var core = ctx.messagesFor(sender.getSender()).getCore();
        String authors = authorFormatter.formatAuthors(rawAuthors, core.getVersionComma(), core.getVersionAnd());

        ctx.sendVersion(
                sender,
                m -> m.getCore().getVersionSuccess(),
                Map.of("version", version, "authors", authors)
        );
        if (sender.getSender().hasPermission(Permissions.RELOAD)) {
            updateChecks.pendingUpdate().ifPresent(update -> sendUpdateNotice(sender, update));
        }
    }

    private void sendUpdateNotice(CommandSourceStack sender, UpdateAvailable update) {
        var tags = Map.of(
                "latest", update.latest(),
                "current", update.current(),
                "url", update.url(),
                "modrinth-url", update.modrinthUrl());
        ctx.sendMessage(sender, m -> m.getCore().getUpdateAvailable(), tags);
    }
}
