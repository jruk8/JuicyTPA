package com.jruk8.juicytpa.core;

import com.jruk8.juicytpa.core.commands.CommandContext;
import com.jruk8.juicytpa.core.commands.CommandsRegistrar;
import com.jruk8.juicytpa.core.commands.tpa.TpaModeStore;
import com.jruk8.juicytpa.core.commands.tpa.TpaService;
import com.jruk8.juicytpa.core.configs.ConfigRegistrar;
import com.jruk8.juicytpa.core.messages.MessageBootstrap;
import com.jruk8.juicytpa.core.placeholders.PlaceholdersBootstrap;
import com.jruk8.juicytpa.core.placeholders.TpaUsageTracker;
import com.jruk8.juicytpa.core.sounds.SoundPlayer;
import com.jruk8.juicytpa.core.storage.StorageBootstrap;
import com.jruk8.juicytpa.core.update.UpdateCheckerBootstrap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class JuicyTPAPlugin extends JavaPlugin {

    private StorageBootstrap storageBootstrap;
    private TpaModeStore modeStore;

    @Override
    public void onEnable() {
        // Set up all core modules
        setupCoreModules();
        StartupBanner.print(
                Bukkit.getConsoleSender()::sendMessage, getName(), getPluginMeta().getVersion());
    }

    @Override
    public void onDisable() {
        if (this.storageBootstrap != null) {
            this.storageBootstrap.shutdown();
        }
    }

    private void setupCoreModules() {
        var logger = getLogger();

        // Initialize reloader
        Reloader reloader = new Reloader(logger);

        // Initialize all configs
        var configRegistrar = new ConfigRegistrar(this);
        configRegistrar.register();
        reloader.register(configRegistrar);

        // Initialize messenger
        var messageRegistrar = new MessageBootstrap(configRegistrar);
        messageRegistrar.register();
        var messenger = messageRegistrar.getMessenger();

        // Initialize sound player
        var soundPlayer = new SoundPlayer(logger);

        // Initialize update checker
        var updateChecker = new UpdateCheckerBootstrap(
                this,
                configRegistrar.getPluginConfig(),
                messenger,
                configRegistrar.getMessagesConfig(),
                logger
        );
        updateChecker.register();
        reloader.register(updateChecker);

        // Initialize storage and placeholders first: commands record usage stats
        TpaUsageTracker usageTracker = setupStorageAndPlaceholders(logger, reloader, configRegistrar);

        // Initialize commands
        var context = new CommandContext(
                configRegistrar.getMessagesConfig(),
                configRegistrar.getSoundsConfig(),
                messenger,
                soundPlayer);
        var tpaService = tpaService(context, configRegistrar, usageTracker);
        setupCommands(reloader, configRegistrar, context, updateChecker, tpaService);

        // Initialize bStats unless anonymous statistics are disabled
        setupMetrics(configRegistrar);
    }

    private void setupMetrics(ConfigRegistrar configRegistrar) {
        boolean metricsEnabled = configRegistrar.getPluginConfig().getMetrics().isEnabled();
        var metricsBootstrap = new MetricsBootstrap(this, metricsEnabled);
        metricsBootstrap.register();
    }

    private TpaService tpaService(
            CommandContext context, ConfigRegistrar configRegistrar, TpaUsageTracker usageTracker) {
        return new TpaService(
                context,
                configRegistrar.getPluginConfig(),
                this.modeStore,
                usageTracker,
                this,
                (task, delay) -> Bukkit.getGlobalRegionScheduler().runDelayed(this, scheduled -> task.run(), delay),
                Bukkit::getPlayer);
    }

    private void setupCommands(
            Reloader reloader,
            ConfigRegistrar configRegistrar,
            CommandContext context,
            UpdateCheckerBootstrap updateChecker,
            TpaService tpaService
    ) {
        var commandRegistrar = new CommandsRegistrar(
                getLifecycleManager(), reloader, context, getPluginMeta(),
                configRegistrar.getPluginConfig(), updateChecker.getService(), tpaService, this);
        commandRegistrar.register();
    }

    private TpaUsageTracker setupStorageAndPlaceholders(
            Logger logger, Reloader reloader, ConfigRegistrar configRegistrar) {
        // Initialize SQLite storage
        this.storageBootstrap = new StorageBootstrap(configRegistrar.getPluginConfig(), getDataFolder(), logger);
        this.storageBootstrap.register();
        reloader.register(this.storageBootstrap);
        var usageTracker = new TpaUsageTracker(this.storageBootstrap::getStorage);
        this.modeStore = new TpaModeStore(this.storageBootstrap::getStorage);

        // Initialize PlaceholderAPI placeholders
        var placeholdersBootstrap = new PlaceholdersBootstrap(usageTracker, this.modeStore, this, logger);
        placeholdersBootstrap.register();
        reloader.register(placeholdersBootstrap);
        return usageTracker;
    }
}
