package com.guessmelody.exception;

import lombok.Getter;

@Getter
public class SpotifyApiException extends RuntimeException {

    private final int statusCode;
    private final String spotifyBody;
    private final int retryAfterSeconds;

    public SpotifyApiException(String message) {
        this(0, message, null, 0);
    }

    public SpotifyApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.spotifyBody = null;
        this.retryAfterSeconds = 0;
    }

    public SpotifyApiException(int statusCode, String message, String spotifyBody) {
        this(statusCode, message, spotifyBody, 0);
    }

    public SpotifyApiException(int statusCode, String message, String spotifyBody, int retryAfterSeconds) {
        super(message);
        this.statusCode = statusCode;
        this.spotifyBody = spotifyBody;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
