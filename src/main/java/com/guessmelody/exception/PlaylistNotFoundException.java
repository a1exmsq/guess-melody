package com.guessmelody.exception;

public class PlaylistNotFoundException extends GameException {

    public PlaylistNotFoundException(String playlistId) {
        super("Playlist " + playlistId + " was not found on Spotify or is not accessible");
    }
}
