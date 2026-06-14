package com.guessmelody.util;

import java.security.SecureRandom;

/**
 * Generates short, human-readable room codes such as "K3P9M".
 *
 * Uses SecureRandom so codes are unpredictable.
 */
public final class RoomCodeGenerator {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RoomCodeGenerator() {
        // Utility class
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
