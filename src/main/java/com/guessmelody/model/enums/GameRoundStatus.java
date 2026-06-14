package com.guessmelody.model.enums;

/**
 * Status of a single game round on the backend.
 */
public enum GameRoundStatus {
    WAITING,    // Round created, waiting for the host to start playback
    PLAYING,    // Track is playing, players can guess
    ENDED       // Round ended because someone guessed or time ran out
}
