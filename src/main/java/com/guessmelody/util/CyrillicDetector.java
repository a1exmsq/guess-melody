package com.guessmelody.util;

import java.util.regex.Pattern;

/**
 * Detects Cyrillic characters in a string.
 *
 * Used by playlist analytics to classify tracks as Cyrillic vs. non-Cyrillic.
 * The Unicode range \u0400-\u04FF covers Russian, Ukrainian, Belarusian, etc.
 */
public final class CyrillicDetector {

    private static final Pattern CYRILLIC_PATTERN = Pattern.compile("[\\u0400-\\u04FF]+");

    private CyrillicDetector() {
        // Utility class
    }

    public static boolean containsCyrillic(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return CYRILLIC_PATTERN.matcher(text).find();
    }
}
