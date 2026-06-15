package com.guessmelody.exception;

public class RoomNotFoundException extends GameException {

    public RoomNotFoundException(String roomCode) {
        super("Room " + roomCode + " not found");
    }
}
