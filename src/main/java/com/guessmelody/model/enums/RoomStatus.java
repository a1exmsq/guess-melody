package com.guessmelody.model.enums;

/**
 * Lifecycle status of a persisted game room.
 */
public enum RoomStatus {
    LOBBY,      // Room created, host configures the playlist and players join
    IN_GAME,    // Active game with rounds and guesses
    FINISHED    // Game over, results shown
}
