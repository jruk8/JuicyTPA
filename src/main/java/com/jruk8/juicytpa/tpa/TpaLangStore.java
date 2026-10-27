package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.message.TpaLanguages;
import com.jruk8.juicytpa.storage.SqliteStorage;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Persists per-player language pins as player-stats integers. A pin
 * freezes the player's language; an absent (or cleared) row means
 * the language keeps following the client locale.
 */
public final class TpaLangStore {

    /** Player-stats key holding the pinned language id. */
    public static final String LANG_KEY = "jtpa_lang";

    private final Supplier<SqliteStorage> storage;

    /**
     * @param storage resolves the live storage; reloads swap the instance,
     * so holders must not keep a direct reference
     */
    public TpaLangStore(Supplier<SqliteStorage> storage) {
        this.storage = storage;
    }

    /**
     * @param player the player
     * @return the pinned code, or empty when the player follows their client
     */
    public Optional<String> language(UUID player) {
        return storage.get().playerValue(LANG_KEY, player).flatMap(TpaLanguages::codeOf);
    }

    /**
     * Pins one player's language.
     *
     * @param player the player
     * @param code a supported language code
     */
    public void setLanguage(UUID player, String code) {
        storage.get().setPlayerValue(LANG_KEY, player, TpaLanguages.idOf(code));
    }

    /**
     * Clears one player's pin. SqliteStorage has no delete, so the
     * sentinel -1 (never a valid id) means follow-the-client again.
     *
     * @param player the player
     */
    public void clearLanguage(UUID player) {
        storage.get().setPlayerValue(LANG_KEY, player, -1);
    }
}
