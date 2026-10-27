package com.jruk8.juicytpa.message;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The language codes JuicyTPA ships, plus client-locale mapping.
 * <p>
 * Codes double as {@code lang/<code>.yml} file names. Regional
 * variants carry their region ({@code pt_br}, {@code zh_tw}).
 * {@code no} covers both Norwegian written standards. The
 * {@link #SUPPORTED} order is stable storage (player pins persist as
 * indexes), so append new codes and never reorder.
 */
public final class TpaLanguages {

    /** Fallback everywhere: missing files, keys and players all read English. */
    public static final String ENGLISH = "en";

    /** All supported codes in storage order. */
    public static final List<String> SUPPORTED = List.of(
            "en", "fi", "de", "es", "fr", "zh_cn", "ru", "pt_br", "ja", "pl",
            "tr", "it", "ko", "nl", "uk", "sv", "no",
            "pt_pt", "zh_tw", "da", "cs", "hu", "vi", "id",
            "ar", "hi", "el", "sk", "ro", "bg", "he", "th", "ms", "lt",
            "lv", "et", "hr", "sr", "bs", "sl", "mk", "ca", "gl", "is",
            "ga", "ka", "hy", "az", "kk", "lol_us", "en_pt", "qya_aa");

    private static final Map<String, String> NATIVE_NAMES = Map.ofEntries(
            Map.entry("en", "English"),
            Map.entry("fi", "Suomi"),
            Map.entry("de", "Deutsch"),
            Map.entry("es", "Español"),
            Map.entry("fr", "Français"),
            Map.entry("zh_cn", "简体中文"),
            Map.entry("ru", "Русский"),
            Map.entry("pt_br", "Português"),
            Map.entry("ja", "日本語"),
            Map.entry("pl", "Polski"),
            Map.entry("tr", "Türkçe"),
            Map.entry("it", "Italiano"),
            Map.entry("ko", "한국어"),
            Map.entry("nl", "Nederlands"),
            Map.entry("uk", "Українська"),
            Map.entry("sv", "Svenska"),
            Map.entry("no", "Norsk"),
            Map.entry("pt_pt", "Português"),
            Map.entry("zh_tw", "繁體中文"),
            Map.entry("da", "Dansk"),
            Map.entry("cs", "Čeština"),
            Map.entry("hu", "Magyar"),
            Map.entry("vi", "Tiếng Việt"),
            Map.entry("id", "Bahasa Indonesia"),
            Map.entry("ar", "العربية"),
            Map.entry("hi", "हिन्दी"),
            Map.entry("el", "Ελληνικά"),
            Map.entry("sk", "Slovenčina"),
            Map.entry("ro", "Română"),
            Map.entry("bg", "Български"),
            Map.entry("he", "עברית"),
            Map.entry("th", "ไทย"),
            Map.entry("ms", "Bahasa Melayu"),
            Map.entry("lt", "Lietuvių"),
            Map.entry("lv", "Latviešu"),
            Map.entry("et", "Eesti"),
            Map.entry("hr", "Hrvatski"),
            Map.entry("sr", "Српски"),
            Map.entry("bs", "Bosanski"),
            Map.entry("sl", "Slovenščina"),
            Map.entry("mk", "Македонски"),
            Map.entry("ca", "Català"),
            Map.entry("gl", "Galego"),
            Map.entry("is", "Íslenska"),
            Map.entry("ga", "Gaeilge"),
            Map.entry("ka", "ქართული"),
            Map.entry("hy", "Հայերեն"),
            Map.entry("az", "Azərbaycanca"),
            Map.entry("kk", "Қазақша"),
            Map.entry("lol_us", "LOLCAT"),
            Map.entry("en_pt", "Pirate Speak"),
            Map.entry("qya_aa", "Quenya"));

    private static final Map<String, String> ENGLISH_NAMES = Map.ofEntries(
            Map.entry("en", "English"),
            Map.entry("fi", "Finnish"),
            Map.entry("de", "German"),
            Map.entry("es", "Spanish"),
            Map.entry("fr", "French"),
            Map.entry("zh_cn", "Chinese (Simplified)"),
            Map.entry("ru", "Russian"),
            Map.entry("pt_br", "Portuguese (Brazil)"),
            Map.entry("ja", "Japanese"),
            Map.entry("pl", "Polish"),
            Map.entry("tr", "Turkish"),
            Map.entry("it", "Italian"),
            Map.entry("ko", "Korean"),
            Map.entry("nl", "Dutch"),
            Map.entry("uk", "Ukrainian"),
            Map.entry("sv", "Swedish"),
            Map.entry("no", "Norwegian"),
            Map.entry("pt_pt", "Portuguese (Portugal)"),
            Map.entry("zh_tw", "Chinese (Traditional)"),
            Map.entry("da", "Danish"),
            Map.entry("cs", "Czech"),
            Map.entry("hu", "Hungarian"),
            Map.entry("vi", "Vietnamese"),
            Map.entry("id", "Indonesian"),
            Map.entry("ar", "Arabic"),
            Map.entry("hi", "Hindi"),
            Map.entry("el", "Greek"),
            Map.entry("sk", "Slovak"),
            Map.entry("ro", "Romanian"),
            Map.entry("bg", "Bulgarian"),
            Map.entry("he", "Hebrew"),
            Map.entry("th", "Thai"),
            Map.entry("ms", "Malay"),
            Map.entry("lt", "Lithuanian"),
            Map.entry("lv", "Latvian"),
            Map.entry("et", "Estonian"),
            Map.entry("hr", "Croatian"),
            Map.entry("sr", "Serbian"),
            Map.entry("bs", "Bosnian"),
            Map.entry("sl", "Slovenian"),
            Map.entry("mk", "Macedonian"),
            Map.entry("ca", "Catalan"),
            Map.entry("gl", "Galician"),
            Map.entry("is", "Icelandic"),
            Map.entry("ga", "Irish"),
            Map.entry("ka", "Georgian"),
            Map.entry("hy", "Armenian"),
            Map.entry("az", "Azerbaijani"),
            Map.entry("kk", "Kazakh"),
            Map.entry("lol_us", "LOLCAT"),
            Map.entry("en_pt", "Pirate Speak"),
            Map.entry("qya_aa", "Quenya"));

    /** Client-locale subtags that map onto a different code. */
    private static final Map<String, String> CLIENT_ALIASES = Map.of(
            "nb", "no", "nn", "no", "in", "id", "iw", "he", "pt", "pt_br", "zh", "zh_cn");

    /** Retired and bare input spellings still accepted by /tpalanguage. */
    private static final Map<String, String> INPUT_ALIASES = Map.of(
            "se", "sv", "portuguese", "pt_br", "chinese", "zh_cn", "中文", "zh_cn");

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
     * Maps a client locale tag onto a code. A tag matching a regional
     * code outright wins ({@code lol_us}, {@code en_pt}); otherwise
     * Portuguese and Chinese look at the region: Portugal reads
     * European Portuguese and Taiwan, Hong Kong and Macau read
     * Traditional Chinese, while every other region reads the
     * Brazilian and Simplified defaults.
     *
     * @param clientLocale the client's locale tag ({@code en_us}, {@code fi-FI}, ...), may be null
     * @return the matching code, or English on anything unknown
     */
    public static String normalize(String clientLocale) {
        if (clientLocale == null || clientLocale.isBlank()) {
            return ENGLISH;
        }
        String tag = clientLocale.strip().toLowerCase(Locale.ROOT).replace('-', '_');
        if (SUPPORTED.contains(tag)) {
            return tag;
        }
        String[] parts = tag.split("_");
        if ("pt".equals(parts[0])) {
            return parts.length > 1 && "pt".equals(parts[1]) ? "pt_pt" : "pt_br";
        }
        if ("zh".equals(parts[0])) {
            for (int i = 1; i < parts.length; i++) {
                if ("tw".equals(parts[i]) || "hk".equals(parts[i])
                        || "mo".equals(parts[i]) || "hant".equals(parts[i])) {
                    return "zh_tw";
                }
            }
            return "zh_cn";
        }
        String code = CLIENT_ALIASES.getOrDefault(parts[0], parts[0]);
        return SUPPORTED.contains(code) ? code : ENGLISH;
    }

    /**
     * Parses {@code /tpalanguage} input: a code, a client subtag, or an
     * English or native language name, case-insensitively. Dashes read
     * as underscores, so {@code pt-br} matches {@code pt_br}.
     *
     * @param input the raw input, may be null
     * @return the matching code, or empty when nothing matches
     */
    public static Optional<String> matches(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String needle = input.strip().toLowerCase(Locale.ROOT).replace('-', '_');
        if (SUPPORTED.contains(needle)) {
            return Optional.of(needle);
        }
        if (CLIENT_ALIASES.containsKey(needle)) {
            return Optional.of(CLIENT_ALIASES.get(needle));
        }
        if (INPUT_ALIASES.containsKey(needle)) {
            return Optional.of(INPUT_ALIASES.get(needle));
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
