package com.guessmelody.exception;

/**
 * Thrown when a game room with the requested code does not exist.
 */
public class RoomNotFoundException extends GameException {

    public RoomNotFoundException(String roomCode) {
        super("Room " + roomCode + " not found");
    }
}
