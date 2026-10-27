package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.storage.SqliteStorage;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Persists per-player trust: asks from a trusted sender skip the
 * pending queue and teleport at once, like a personal accept-all
 * mode. Here-requests always still need a manual answer.
 */
public final class TpaTrustStore {

    private final Supplier<SqliteStorage> storage;

    /**
     * @param storage resolves the live storage; reloads swap the instance,
     * so holders must not keep a direct reference
     */
    public TpaTrustStore(Supplier<SqliteStorage> storage) {
        this.storage = storage;
    }

    /**
     * Checks whether a player trusts another player.
     *
     * @param truster the player doing the trusting
     * @param trusted the player being trusted
     * @return {@code true} when the trust exists
     */
    public boolean isTrusted(UUID truster, UUID trusted) {
        return storage.get().isTrusted(truster, trusted);
    }

    /**
     * Trusts or untrusts one player.
     *
     * @param truster the player doing the trusting
     * @param trusted the player being trusted
     * @param trustedValue whether the trust should exist afterwards
     */
    public void setTrusted(UUID truster, UUID trusted, boolean trustedValue) {
        if (trustedValue) {
            storage.get().addTrust(truster, trusted);
        } else {
            storage.get().removeTrust(truster, trusted);
        }
    }

    /**
     * @param truster the player doing the trusting
     * @return every trusted player id, oldest first
     */
    public List<UUID> trusted(UUID truster) {
        return storage.get().trusted(truster);
    }
}
