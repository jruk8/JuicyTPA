package com.jruk8.juicytpa.core.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

/**
 * Fetches the latest release from the GitHub Releases API over HTTPS.
 */
public final class GitHubReleaseHttp implements UpdateCheckHttp {

    private static final String OWNER = "jruk8";
    private static final String REPO = "JuicyTPA";
    private static final int TIMEOUT_MILLIS = 10_000;
    private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HTML_URL = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PRERELEASE = Pattern.compile("\"prerelease\"\\s*:\\s*(true|false)");

    private final String userAgent;

    /**
     * Constructs the adapter. GitHub asks callers to identify themselves,
     * so the plugin version goes into the User-Agent header.
     *
     * @param pluginVersion the running plugin version
     */
    public GitHubReleaseHttp(String pluginVersion) {
        this.userAgent = "JuicyTPA/" + pluginVersion + " (update-checker)";
    }

    @Override
    public Optional<GitHubRelease> fetchLatest() throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) URI.create(
                "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest")
                .toURL().openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        try {
            int status = connection.getResponseCode();
            if (noReleasePublished(status)) {
                return Optional.empty();
            }
            if (status < 200 || status >= 300) {
                throw new IOException("GitHub releases returned HTTP " + status);
            }
            try (InputStream body = connection.getInputStream()) {
                return parseLatest(new String(body.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * GitHub answers 404 when no full release exists yet (drafts and
     * prereleases do not count), which is not a failure. Pure for tests.
     *
     * @param statusCode the HTTP status code
     * @return true when the status means nothing is published
     */
    static boolean noReleasePublished(int statusCode) {
        return statusCode == 404;
    }

    /**
     * Reads the tag, page URL and prerelease flag out of a
     * {@code releases/latest} response. Pure for tests.
     *
     * @param json the raw release object
     * @return the release, or empty for prereleases and unparseable bodies
     */
    static Optional<GitHubRelease> parseLatest(String json) {
        String tag = firstMatch(TAG_NAME, json);
        String url = firstMatch(HTML_URL, json);
        if (tag == null || url == null || "true".equals(firstMatch(PRERELEASE, json))) {
            return Optional.empty();
        }
        return Optional.of(new GitHubRelease(tag, url));
    }

    private static String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
}
