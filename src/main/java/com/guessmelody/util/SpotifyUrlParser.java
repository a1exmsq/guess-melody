package com.guessmelody.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts a Spotify playlist id from URLs, URIs, or raw ids.
 *
 * Supported formats:
 * - https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M
 * - spotify:playlist:37i9dQZF1DXcBWIGoYBM5M
 * - 37i9dQZF1DXcBWIGoYBM5M
 */
public final class SpotifyUrlParser {

    private static final Pattern WEB_URL_PATTERN = Pattern.compile(
            "spotify\\.com/playlist/([a-zA-Z0-9]+)"
    );

    private static final Pattern URI_PATTERN = Pattern.compile(
            "spotify:playlist:([a-zA-Z0-9]+)"
    );

    private static final Pattern RAW_ID_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9]{22}$"
    );

    private SpotifyUrlParser() {
        // Utility class
    }

    public static String extractPlaylistId(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Playlist link cannot be empty");
        }

        String trimmed = input.trim();

        if (RAW_ID_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }

        Matcher uriMatcher = URI_PATTERN.matcher(trimmed);
        if (uriMatcher.find()) {
            return uriMatcher.group(1);
        }

        Matcher webMatcher = WEB_URL_PATTERN.matcher(trimmed);
        if (webMatcher.find()) {
            return webMatcher.group(1);
        }

        throw new IllegalArgumentException(
                "Could not parse Spotify playlist link: " + trimmed
        );
    }
}
