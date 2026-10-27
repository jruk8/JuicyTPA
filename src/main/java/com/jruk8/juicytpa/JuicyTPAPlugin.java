package com.jruk8.juicytpa;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;
import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.command.CommandsRegistrar;
import com.jruk8.juicytpa.config.ConfigRegistrar;
import com.jruk8.juicytpa.core.MetricsBootstrap;
import com.jruk8.juicytpa.core.Reloader;
import com.jruk8.juicytpa.core.StartupBanner;
import com.jruk8.juicytpa.hook.LuckPermsMeta;
import com.jruk8.juicytpa.message.MessageBootstrap;
import com.jruk8.juicytpa.message.MessagesConfig;
import com.jruk8.juicytpa.message.Messenger;
import com.jruk8.juicytpa.placeholder.PlaceholdersBootstrap;
import com.jruk8.juicytpa.sound.SoundPlayer;
import com.jruk8.juicytpa.storage.StorageBootstrap;
import com.jruk8.juicytpa.tpa.TpaBlockStore;
import com.jruk8.juicytpa.tpa.TpaCooldowns;
import com.jruk8.juicytpa.tpa.TpaModeStore;
import com.jruk8.juicytpa.tpa.TpaService;
import com.jruk8.juicytpa.tpa.TpaTrustStore;
import com.jruk8.juicytpa.tpa.TpaUsageTracker;
import com.jruk8.juicytpa.tpa.TpaWarnManager;
import com.jruk8.juicytpa.tpa.TpaWarnStore;
import com.jruk8.juicytpa.update.UpdateCheckerBootstrap;

public final class JuicyTPAPlugin extends JavaPlugin {

    private StorageBootstrap storageBootstrap;
    private TpaModeStore modeStore;
    private TpaBlockStore blockStore;
    private TpaTrustStore trustStore;

    @Override
    public void onEnable() {
        // Set up all core modules
        setupCoreModules();
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
        var updateChecker = setupUpdateChecker(reloader, configRegistrar, messenger, logger);

        // Initialize storage and placeholders first: commands record usage stats
        TpaUsageTracker usageTracker = setupStorageAndPlaceholders(logger, reloader, configRegistrar);

        // Initialize commands
        var context = new CommandContext(
                configRegistrar.getMessagesConfig(),
                configRegistrar.getSoundsConfig(),
                messenger,
                soundPlayer);
        var tpaService = tpaService(context, configRegistrar, usageTracker);
        setupPlaceholders(reloader, logger, usageTracker, tpaService, configRegistrar.getMessagesConfig());
        setupCommands(reloader, configRegistrar, context, updateChecker, tpaService);

        // Initialize bStats unless anonymous statistics are disabled
        setupMetrics(configRegistrar);
    }

    private void setupMetrics(ConfigRegistrar configRegistrar) {
        boolean metricsEnabled = configRegistrar.getPluginConfig().getMetrics().isEnabled();
        var metricsBootstrap = new MetricsBootstrap(this, metricsEnabled);
        metricsBootstrap.register();
    }

    private UpdateCheckerBootstrap setupUpdateChecker(
            Reloader reloader, ConfigRegistrar configRegistrar, Messenger messenger, Logger logger) {
        var updateChecker = new UpdateCheckerBootstrap(
                this,
                configRegistrar.getPluginConfig(),
                messenger,
                configRegistrar.getMessagesConfig(),
                logger,
                this::printBanner);
        updateChecker.register();
        reloader.register(updateChecker);
        return updateChecker;
    }

    private TpaService tpaService(
            CommandContext context, ConfigRegistrar configRegistrar, TpaUsageTracker usageTracker) {
        var warns = new TpaWarnManager(
                new TpaWarnStore(this.storageBootstrap::getStorage),
                configRegistrar.getPluginConfig(),
                System::currentTimeMillis);
        var cooldowns = new TpaCooldowns(
                configRegistrar.getPluginConfig(), System::currentTimeMillis, LuckPermsMeta::meta);
        return new TpaService(
                context,
                configRegistrar.getPluginConfig(),
                this.modeStore,
                this.blockStore,
                this.trustStore,
                warns,
                cooldowns,
                usageTracker,
                this,
                (task, delay) -> Bukkit.getGlobalRegionScheduler().runDelayed(this, scheduled -> task.run(), delay),
                Bukkit::getPlayer,
                uuid -> Bukkit.getOfflinePlayer(uuid).getName());
    }

    private void setupPlaceholders(
            Reloader reloader,
            Logger logger,
            TpaUsageTracker usageTracker,
            TpaService tpaService,
            MessagesConfig messages) {
        // PlaceholderAPI placeholders (needs the request service for jtpa_pending)
        var placeholdersBootstrap = new PlaceholdersBootstrap(
                usageTracker, this.modeStore, tpaService, this, messages, logger);
        placeholdersBootstrap.register();
        reloader.register(placeholdersBootstrap);
    }

    private void printBanner(boolean updateAvailable) {
        StartupBanner.print(
                Bukkit.getConsoleSender()::sendMessage,
                getName(),
                getPluginMeta().getVersion(),
                updateAvailable);
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
        this.blockStore = new TpaBlockStore(this.storageBootstrap::getStorage);
        this.trustStore = new TpaTrustStore(this.storageBootstrap::getStorage);
        return usageTracker;
    }
}
