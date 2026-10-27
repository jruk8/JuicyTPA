package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.storage.SqliteStorage;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Persists each player's {@link TpaMode} as its ordinal under one stat
 * key. Absent or unknown values read back as {@link TpaMode#NORMAL}.
 */
public final class TpaModeStore {

    /**
     * Stat key for the per-player TPA mode.
     */
    public static final String MODE_KEY = "jtpa_mode";

    private final Supplier<SqliteStorage> storage;

    /**
     * @param storage resolves the live storage; reloads swap the instance,
     * so holders must not keep a direct reference
     */
    public TpaModeStore(Supplier<SqliteStorage> storage) {
        this.storage = storage;
    }

    /**
     * Reads a player's mode.
     *
     * @param player the player
     * @return the stored mode, or normal when none is stored
     */
    public TpaMode mode(UUID player) {
        return storage.get().playerValue(MODE_KEY, player)
                .flatMap(TpaModeStore::toMode)
                .orElse(TpaMode.NORMAL);
    }

    /**
     * Stores a player's mode.
     *
     * @param player the player
     * @param mode the mode to store
     */
    public void setMode(UUID player, TpaMode mode) {
        storage.get().setPlayerValue(MODE_KEY, player, mode.ordinal());
    }

    private static Optional<TpaMode> toMode(int value) {
        TpaMode[] modes = TpaMode.values();
        if (value < 0 || value >= modes.length) {
            return Optional.empty();
        }
        return Optional.of(modes[value]);
    }
}
