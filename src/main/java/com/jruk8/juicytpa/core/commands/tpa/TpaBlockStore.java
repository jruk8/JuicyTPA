package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.storage.SqliteStorage;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Persists per-player request blocks: a blocked sender can never get a
 * request through, no matter the receiver's mode.
 */
public final class TpaBlockStore {

    private final Supplier<SqliteStorage> storage;

    /**
     * @param storage resolves the live storage; reloads swap the instance,
     * so holders must not keep a direct reference
     */
    public TpaBlockStore(Supplier<SqliteStorage> storage) {
        this.storage = storage;
    }

    /**
     * Checks whether a player blocks requests from another player.
     *
     * @param blocker the player doing the blocking
     * @param blocked the player being blocked
     * @return {@code true} when the block exists
     */
    public boolean isBlocked(UUID blocker, UUID blocked) {
        return storage.get().isBlocked(blocker, blocked);
    }

    /**
     * Blocks or unblocks requests from one player.
     *
     * @param blocker the player doing the blocking
     * @param blocked the player being blocked
     * @param blockedValue whether the block should exist afterwards
     */
    public void setBlocked(UUID blocker, UUID blocked, boolean blockedValue) {
        if (blockedValue) {
            storage.get().addBlock(blocker, blocked);
        } else {
            storage.get().removeBlock(blocker, blocked);
        }
    }
}
