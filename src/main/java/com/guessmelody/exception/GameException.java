package com.guessmelody.exception;

/**
 * Base runtime exception for game logic errors (e.g. an illegal move in a finished round).
 */
public class GameException extends RuntimeException {

    public GameException(String message) {
        super(message);
    }

    public GameException(String message, Throwable cause) {
        super(message, cause);
    }
}
