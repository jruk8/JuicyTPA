package com.jruk8.juicytpa.storage;

import com.jruk8.juicytpa.config.PluginConfig;
import com.jruk8.juicytpa.core.Bootstrap;
import com.jruk8.juicytpa.core.Reloadable;

import java.io.File;
import java.util.logging.Logger;

/**
 * Wires up the SQLite file storage from {@code PluginConfig.Storage}.
 * <p>
 * On {@link #reload()} the existing database is closed and reopened, which also
 * picks up a changed file name from the config.
 */
public final class StorageBootstrap implements Bootstrap, Reloadable {

    private SqliteStorage storage;
    private final SqliteStorage deadStorage;
    private final PluginConfig pluginConfig;
    private final File dataFolder;
    private final Logger logger;

    public StorageBootstrap(PluginConfig pluginConfig, File dataFolder, Logger logger) {
        this.pluginConfig = pluginConfig;
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.deadStorage = new SqliteStorage("jdbc:sqlite::memory:", logger);
    }

    /**
     * Returns the live storage, or a never-opened instance while the
     * database is unavailable (reads degrade to zero, writes are
     * ignored), so holders never see null.
     *
     * @return the current storage
     */
    public SqliteStorage getStorage() {
        return this.storage != null ? this.storage : this.deadStorage;
    }

    @Override
    public void register() {
        open();
    }

    @Override
    public void reload() {
        shutdown();
        open();
    }

    public void shutdown() {
        if (this.storage != null) {
            this.storage.close();
            this.storage = null;
        }
    }

    private void open() {
        String fileName = this.pluginConfig.getStorage().getDbFile();
        File dbFile = new File(this.dataFolder, fileName);
        SqliteStorage candidate = new SqliteStorage(dbFile, this.logger);
        if (candidate.init()) {
            this.storage = candidate;
            this.logger.info("SQLite storage ready at '" + fileName + "'.");
        } else {
            this.storage = null;
            this.logger.severe("SQLite storage unavailable; stats read as 0 and writes are ignored.");
        }
    }
}