package com.jruk8.juicytpa.command.user.version;

import java.util.List;
import java.util.Map;

/**
 * Business logic class for formatting author lists.
 * Follows clean architecture principles - pure formatting logic with no dependencies on framework types.
 * Known authors link to their pages; unknown names render as-is.
 * Joining words come from the reader's bundle, so every language punctuates its own way.
 */
public class AuthorFormatter {

    private static final Map<String, String> AUTHOR_LINKS = Map.of(
            "jruk", "https://github.com/jruk8");

    /**
     * Formats a single author name.
     *
     * @param author the author name
     * @return the author name, linked when a URL is known
     */
    public String formatSingleAuthor(String author) {
        return linked(author);
    }

    /**
     * Formats two authors with the configured "and" between them.
     *
     * @param firstAuthor  the first author
     * @param secondAuthor the second author
     * @param and the "and" word from messages.yml
     * @return formatted string like "MyAuthor <gray>and</gray> MyOther"
     */
    public String formatTwoAuthors(String firstAuthor, String secondAuthor, String and) {
        return linked(firstAuthor) + " " + and + " " + linked(secondAuthor);
    }

    /**
     * Formats three or more authors with the configured comma and "and".
     * Format: "MyAuthor<gray>,</gray> MyOther<gray>,</gray> <gray>and</gray> MyOtherOther"
     *
     * @param authors list of author names (must have at least 3 elements)
     * @param comma the comma separator from messages.yml
     * @param and the "and" word from messages.yml
     * @return formatted string with commas and "and"
     */
    public String formatThreePlusAuthors(List<String> authors, String comma, String and) {
        if (authors.size() < 3) {
            return formatTwoAuthors(authors.get(0), authors.get(1), and);
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < authors.size(); i++) {
            String author = linked(authors.get(i));

            if (i == 0) {
                result.append(author);
            } else if (i == authors.size() - 1) {
                // Last author with comma and "and"
                result.append(comma).append(" ").append(and).append(" ").append(author);
            } else {
                // Middle authors with comma
                result.append(comma).append(" ").append(author);
            }
        }

        return result.toString();
    }

    private static String linked(String author) {
        String url = AUTHOR_LINKS.get(author);
        if (url == null) {
            return author;
        }
        return "<click:open_url:'" + url + "'><hover:show_text:'" + url + "'>" + author + "</hover></click>";
    }

    /**
     * Formats authors list based on the number of authors.
     *
     * @param authors list of author names
     * @param comma the comma separator from messages.yml
     * @param and the "and" word from messages.yml
     * @return properly formatted author string
     */
    public String formatAuthors(List<String> authors, String comma, String and) {
        if (authors.isEmpty()) {
            return "";
        }

        int count = authors.size();
        if (count == 1) {
            return linked(authors.getFirst());
        } else if (count == 2) {
            return formatTwoAuthors(authors.get(0), authors.get(1), and);
        }
        return formatThreePlusAuthors(authors, comma, and);
    }
}
