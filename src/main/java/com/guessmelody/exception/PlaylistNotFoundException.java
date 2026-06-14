package com.guessmelody.exception;

/**
 * Thrown when a Spotify playlist cannot be found or parsed.
 */
public class PlaylistNotFoundException extends GameException {

    public PlaylistNotFoundException(String playlistId) {
        super("Playlist " + playlistId + " was not found on Spotify or is not accessible");
    }
}
