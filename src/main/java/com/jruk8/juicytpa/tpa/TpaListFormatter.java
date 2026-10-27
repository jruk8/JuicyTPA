package com.jruk8.juicytpa.tpa;

import java.util.List;

/**
 * Oxford-style name lists for {@code /tpalist}, mirroring the sibling
 * JManhunt formatter: two names use "a and b", three or more use the
 * Oxford comma. Past ten names the tail truncates to an "and N more"
 * ending instead. Separators come from messages.yml, so every word is
 * configurable.
 */
public final class TpaListFormatter {

    private static final int MAX_SHOWN = 10;

    private TpaListFormatter() {
    }

    /**
     * Joins already-linked names with an Oxford "and" before the last
     * entry, truncating past ten names.
     *
     * @param names the display-ready names, never empty at call sites
     * @param comma the separator between names, e.g. "{hl},"
     * @param and the word before the last name, e.g. "{hl}and"
     * @param more the overflow tail with a {count} slot, e.g. "{plain}, and {hl}{count} more."
     * @return the joined names
     */
    public static String join(List<String> names, String comma, String and, String more) {
        if (names.size() > MAX_SHOWN) {
            int hidden = names.size() - MAX_SHOWN;
            return String.join(comma + " ", names.subList(0, MAX_SHOWN))
                    + more.replace("{count}", String.valueOf(hidden));
        }
        return switch (names.size()) {
            case 0 -> "";
            case 1 -> names.getFirst();
            case 2 -> names.get(0) + " " + and + " " + names.get(1);
            default -> String.join(comma + " ", names.subList(0, names.size() - 1))
                    + comma + " " + and + " " + names.getLast();
        };
    }
}
