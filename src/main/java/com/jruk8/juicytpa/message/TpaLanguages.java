package com.jruk8.juicytpa.message;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The language codes JuicyTPA ships, plus client-locale mapping.
 * <p>
 * Codes are short ({@code en}, {@code fi}, ...) and double as
 * {@code lang/<code>.yml} file names. {@code se} is Swedish and
 * {@code no} covers both Norwegian written standards. The
 * {@link #SUPPORTED} order is stable storage (player pins persist as
 * indexes), so append new codes and never reorder.
 */
public final class TpaLanguages {

    /** Fallback everywhere: missing files, keys and players all read English. */
    public static final String ENGLISH = "en";

    /** All supported codes in storage order. */
    public static final List<String> SUPPORTED = List.of(
            "en", "fi", "de", "es", "fr", "zh", "ru", "pt", "ja", "pl",
            "tr", "it", "ko", "nl", "uk", "se", "no");

    private static final Map<String, String> NATIVE_NAMES = Map.ofEntries(
            Map.entry("en", "English"),
            Map.entry("fi", "Suomi"),
            Map.entry("de", "Deutsch"),
            Map.entry("es", "Español"),
            Map.entry("fr", "Français"),
            Map.entry("zh", "中文"),
            Map.entry("ru", "Русский"),
            Map.entry("pt", "Português"),
            Map.entry("ja", "日本語"),
            Map.entry("pl", "Polski"),
            Map.entry("tr", "Türkçe"),
            Map.entry("it", "Italiano"),
            Map.entry("ko", "한국어"),
            Map.entry("nl", "Nederlands"),
            Map.entry("uk", "Українська"),
            Map.entry("se", "Svenska"),
            Map.entry("no", "Norsk"));

    private static final Map<String, String> ENGLISH_NAMES = Map.ofEntries(
            Map.entry("en", "English"),
            Map.entry("fi", "Finnish"),
            Map.entry("de", "German"),
            Map.entry("es", "Spanish"),
            Map.entry("fr", "French"),
            Map.entry("zh", "Chinese"),
            Map.entry("ru", "Russian"),
            Map.entry("pt", "Portuguese"),
            Map.entry("ja", "Japanese"),
            Map.entry("pl", "Polish"),
            Map.entry("tr", "Turkish"),
            Map.entry("it", "Italian"),
            Map.entry("ko", "Korean"),
            Map.entry("nl", "Dutch"),
            Map.entry("uk", "Ukrainian"),
            Map.entry("se", "Swedish"),
            Map.entry("no", "Norwegian"));

    /** Client-locale subtags that map onto a different code. */
    private static final Map<String, String> CLIENT_ALIASES = Map.of(
            "sv", "se", "nb", "no", "nn", "no");

    private TpaLanguages() {
    }

    /**
     * @param code the code to check
     * @return whether JuicyTPA ships that language
     */
    public static boolean isSupported(String code) {
        return code != null && SUPPORTED.contains(code);
    }

    /**
     * Maps a client locale tag onto a code.
     *
     * @param clientLocale the client's locale tag ({@code en_us}, {@code fi-FI}, ...), may be null
     * @return the matching code, or English on anything unknown
     */
    public static String normalize(String clientLocale) {
        if (clientLocale == null || clientLocale.isBlank()) {
            return ENGLISH;
        }
        String subtag = clientLocale.strip().toLowerCase(Locale.ROOT).split("[_-]", 2)[0];
        String code = CLIENT_ALIASES.getOrDefault(subtag, subtag);
        return SUPPORTED.contains(code) ? code : ENGLISH;
    }

    /**
     * Parses {@code /tpalanguage} input: a code, a client subtag, or an
     * English or native language name, case-insensitively.
     *
     * @param input the raw input, may be null
     * @return the matching code, or empty when nothing matches
     */
    public static Optional<String> matches(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String needle = input.strip().toLowerCase(Locale.ROOT);
        if (SUPPORTED.contains(needle)) {
            return Optional.of(needle);
        }
        if (CLIENT_ALIASES.containsKey(needle)) {
            return Optional.of(CLIENT_ALIASES.get(needle));
        }
        for (String code : SUPPORTED) {
            if (ENGLISH_NAMES.get(code).toLowerCase(Locale.ROOT).equals(needle)
                    || NATIVE_NAMES.get(code).toLowerCase(Locale.ROOT).equals(needle)) {
                return Optional.of(code);
            }
        }
        return Optional.empty();
    }

    /**
     * @param code the language code
     * @return the native display name, or English on unknown codes
     */
    public static String nativeName(String code) {
        return NATIVE_NAMES.getOrDefault(code, NATIVE_NAMES.get(ENGLISH));
    }

    /**
     * @param code the language code
     * @return the display label, e.g. {@code Suomi (FI)}
     */
    public static String label(String code) {
        return nativeName(code) + " (" + code.toUpperCase(Locale.ROOT) + ")";
    }

    /**
     * @param code a supported code
     * @return the stable storage id
     */
    public static int idOf(String code) {
        int id = SUPPORTED.indexOf(code);
        if (id < 0) {
            throw new IllegalArgumentException("Unknown language: " + code);
        }
        return id;
    }

    /**
     * @param id a stored id
     * @return the code, or empty when the id is unknown
     */
    public static Optional<String> codeOf(int id) {
        return id >= 0 && id < SUPPORTED.size() ? Optional.of(SUPPORTED.get(id)) : Optional.empty();
    }
}
