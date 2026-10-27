package com.jruk8.juicytpa.placeholder;

import com.jruk8.juicytpa.core.Bootstrap;
import com.jruk8.juicytpa.core.Reloadable;
import com.jruk8.juicytpa.message.MessagesConfig;
import com.jruk8.juicytpa.tpa.TpaModeStore;
import com.jruk8.juicytpa.tpa.TpaService;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;
import com.jruk8.juicytpa.tpa.TpaUsageTracker;

/**
 * Registers and tears down the PlaceholderAPI expansion.
 * <p>
 * When PlaceholderAPI is not installed, registration is skipped gracefully.
 * {@link #reload()} re-registers everything.
 */
public final class PlaceholdersBootstrap implements Bootstrap, Reloadable {

    private static final String PLACEHOLDER_API_NAME = "PlaceholderAPI";

    private final TpaUsageTracker tracker;
    private final TpaModeStore modes;
    private final TpaService service;
    private final JavaPlugin plugin;
    private final MessagesConfig messages;
    private final Logger logger;

    private TpaExpansion expansion;

    public PlaceholdersBootstrap(
            TpaUsageTracker tracker,
            TpaModeStore modes,
            TpaService service,
            JavaPlugin plugin,
            MessagesConfig messages,
            Logger logger) {
        this.tracker = tracker;
        this.modes = modes;
        this.service = service;
        this.plugin = plugin;
        this.messages = messages;
        this.logger = logger;
    }

    @Override
    public void register() {
        if (this.plugin.getServer().getPluginManager().getPlugin(PLACEHOLDER_API_NAME) == null) {
            this.logger.info("PlaceholderAPI is not installed; skipping placeholder registration.");
            return;
        }
        this.expansion = new TpaExpansion(
                this.tracker,
                this.modes,
                this.service,
                this.plugin,
                this.messages,
                name -> Bukkit.getOfflinePlayer(name).getUniqueId());
        if (this.expansion.register()) {
            this.logger.info("Registered placeholders with identifier '" + TpaExpansion.IDENTIFIER + "'.");
        } else {
            unregister();
            this.logger.warning("PlaceholderAPI rejected the expansion registration.");
        }
    }

    @Override
    public void reload() {
        unregister();
        register();
    }

    public boolean unregister() {
        if (this.expansion != null) {
            this.expansion.unregister();
            this.expansion = null;
        }
        return true;
    }
}
