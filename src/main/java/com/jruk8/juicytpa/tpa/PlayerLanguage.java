package com.jruk8.juicytpa.tpa;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player language pins with an in-memory cache: every send
 * resolves the recipient's language, so pins must not hit the
 * database each time. All writes go through here, so the cache
 * never goes stale. Placeholders may resolve off-thread, hence
 * the concurrent map.
 */
public final class PlayerLanguage {

    private final TpaLangStore store;
    private final Map<UUID, Optional<String>> cache = new ConcurrentHashMap<>();

    public PlayerLanguage(TpaLangStore store) {
        this.store = store;
    }

    /**
     * @param player the player
     * @return the pinned code, or null to follow the client locale
     */
    public String get(UUID player) {
        return cache.computeIfAbsent(player, key -> store.language(key)).orElse(null);
    }

    /**
     * Pins one player's language until explicitly cleared.
     *
     * @param player the player
     * @param code a supported language code
     */
    public void set(UUID player, String code) {
        store.setLanguage(player, code);
        cache.put(player, Optional.of(code));
    }

    /**
     * Clears one player's pin so the language follows the client again.
     *
     * @param player the player
     */
    public void clear(UUID player) {
        store.clearLanguage(player);
        cache.put(player, Optional.empty());
    }
}
