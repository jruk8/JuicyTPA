package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.storage.SqliteStorage;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Persists the per-player warning statistics: unanswered (expired) and
 * denied request counts plus the seconds those requests sat pending.
 * {@link TpaWarnManager} reads these to flag AFK players and frequent
 * cancellers.
 */
public final class TpaWarnStore {

    /**
     * Stat key for expired requests per player.
     */
    public static final String UNANSWERED_KEY = "jtpa_warn_unanswered";

    /**
     * Stat key for cumulative seconds with any request pending per player.
     */
    public static final String PENDING_SECONDS_KEY = "jtpa_warn_pending_seconds";

    /**
     * Stat key for denied requests per player.
     */
    public static final String DENIED_KEY = "jtpa_warn_denied";

    /**
     * Stat key for cumulative seconds denied requests sat pending per player.
     */
    public static final String DENIED_SECONDS_KEY = "jtpa_warn_denied_seconds";

    private final Supplier<SqliteStorage> storage;

    /**
     * @param storage resolves the live storage; reloads swap the instance,
     * so holders must not keep a direct reference
     */
    public TpaWarnStore(Supplier<SqliteStorage> storage) {
        this.storage = storage;
    }

    /**
     * Records one expired request and its pending time.
     *
     * @param player the player that let the request expire
     * @param seconds how long the request sat pending
     */
    public void recordExpiry(UUID player, long seconds) {
        this.storage.get().increment(UNANSWERED_KEY, player);
        add(PENDING_SECONDS_KEY, player, seconds);
    }

    /**
     * Records one denied request and its pending time.
     *
     * @param player the player that denied the request
     * @param seconds how long the request sat pending
     */
    public void recordDenied(UUID player, long seconds) {
        this.storage.get().increment(DENIED_KEY, player);
        add(DENIED_SECONDS_KEY, player, seconds);
    }

    /**
     * Adds wall-clock seconds the player had any request pending.
     *
     * @param player the player
     * @param seconds the seconds to add
     */
    public void addPendingSeconds(UUID player, long seconds) {
        add(PENDING_SECONDS_KEY, player, seconds);
    }

    /**
     * @param player the player
     * @return the expired request count, or 0 when none is stored
     */
    public int unanswered(UUID player) {
        return this.storage.get().playerTotal(UNANSWERED_KEY, player);
    }

    /**
     * @param player the player
     * @return the cumulative pending seconds, or 0 when none is stored
     */
    public int pendingSeconds(UUID player) {
        return this.storage.get().playerTotal(PENDING_SECONDS_KEY, player);
    }

    /**
     * Clears the AFK statistics: the player showed intent by answering.
     *
     * @param player the player that answered
     */
    public void resetUnanswered(UUID player) {
        var store = this.storage.get();
        store.setPlayerValue(UNANSWERED_KEY, player, 0);
        store.setPlayerValue(PENDING_SECONDS_KEY, player, 0);
    }

    /**
     * Clears the canceller statistics: an accept breaks the chain.
     *
     * @param player the player that accepted
     */
    public void resetDenied(UUID player) {
        var store = this.storage.get();
        store.setPlayerValue(DENIED_KEY, player, 0);
        store.setPlayerValue(DENIED_SECONDS_KEY, player, 0);
    }

    /**
     * @param player the player
     * @return the denied request count, or 0 when none is stored
     */
    public int denied(UUID player) {
        return this.storage.get().playerTotal(DENIED_KEY, player);
    }

    /**
     * @param player the player
     * @return the cumulative denied pending seconds, or 0 when none is stored
     */
    public int deniedSeconds(UUID player) {
        return this.storage.get().playerTotal(DENIED_SECONDS_KEY, player);
    }

    private void add(String key, UUID player, long seconds) {
        var store = this.storage.get();
        store.setPlayerValue(key, player, store.playerTotal(key, player) + (int) Math.max(0, seconds));
    }
}
