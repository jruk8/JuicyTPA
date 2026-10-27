package com.jruk8.juicytpa.api;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * How a player receives TP requests. Persisted per player in the database,
 * defaulting to {@link #NORMAL}.
 */
public enum TpaMode {

    /** Requests arrive normally and wait for /tpaccept or /tpadeny. */
    NORMAL,

    /** Incoming requests are refused at once; the sender is told. */
    BLOCK_ALL,

    /** Incoming requests teleport at once, as if accepted. */
    ACCEPT_ALL;

    /**
     * @return the mode after this one when argless /tpamode cycles
     */
    public TpaMode next() {
        return switch (this) {
            case NORMAL -> BLOCK_ALL;
            case BLOCK_ALL -> ACCEPT_ALL;
            case ACCEPT_ALL -> NORMAL;
        };
    }

    /**
     * Parses a /tpamode argument: full names plus the block/accept
     * shorthands, case-insensitive.
     *
     * @param input the raw argument
     * @return the mode, or empty when the input matches nothing
     */
    public static Optional<TpaMode> parse(String input) {
        String token = input.trim().toLowerCase(Locale.ROOT);
        return switch (token) {
            case "normal" -> Optional.of(NORMAL);
            case "block_all", "block" -> Optional.of(BLOCK_ALL);
            case "accept_all", "accept" -> Optional.of(ACCEPT_ALL);
            default -> Optional.empty();
        };
    }

    /**
     * @return the completable mode names, longest first
     */
    public static List<String> names() {
        return List.of("accept_all", "block_all", "normal");
    }
}
