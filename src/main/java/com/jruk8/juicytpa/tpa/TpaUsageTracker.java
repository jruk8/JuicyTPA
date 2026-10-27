package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.storage.SqliteStorage;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Domain service for the request statistics behind the {@code jtpa}
 * placeholders. Keeps the DB stat keys in one place and exposes the
 * read/write operations the expansion and commands need.
 */
public final class TpaUsageTracker {

    /**
     * Stat key for TP requests sent per player.
     */
    public static final String SENT_KEY = "jtpa_sent";

    /**
     * Stat key for TP requests received per player.
     */
    public static final String RECEIVED_KEY = "jtpa_received";

    private final Supplier<SqliteStorage> storage;

    /**
     * @param storage resolves the live storage; reloads swap the instance,
     * so holders must not keep a direct reference
     */
    public TpaUsageTracker(Supplier<SqliteStorage> storage) {
        this.storage = storage;
    }

    /**
     * Records one sent request, per player and globally.
     *
     * @param player the player that sent the request
     */
    public void recordSent(UUID player) {
        this.storage.get().increment(SENT_KEY, player);
        this.storage.get().incrementGlobal(SENT_KEY);
    }

    /**
     * Records one received request, per player and globally.
     *
     * @param player the player that received the request
     */
    public void recordReceived(UUID player) {
        this.storage.get().increment(RECEIVED_KEY, player);
        this.storage.get().incrementGlobal(RECEIVED_KEY);
    }

    public int playerSent(UUID player) {
        return this.storage.get().playerTotal(SENT_KEY, player);
    }

    public int playerReceived(UUID player) {
        return this.storage.get().playerTotal(RECEIVED_KEY, player);
    }

    public int globalSent() {
        return this.storage.get().globalTotal(SENT_KEY);
    }

    public int globalReceived() {
        return this.storage.get().globalTotal(RECEIVED_KEY);
    }
}
