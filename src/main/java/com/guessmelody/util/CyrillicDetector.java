package com.guessmelody.util;

import java.util.regex.Pattern;

public final class CyrillicDetector {

    private static final Pattern CYRILLIC_PATTERN = Pattern.compile("[\\u0400-\\u04FF]+");

    private CyrillicDetector() {
    }

    public static boolean containsCyrillic(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return CYRILLIC_PATTERN.matcher(text).find();
    }
}
