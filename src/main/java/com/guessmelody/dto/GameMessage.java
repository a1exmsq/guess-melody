package com.guessmelody.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * WebSocket message envelope used for every game event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage {

    public enum Type {
        ROOM_CREATED,      // Room was created
        PLAYER_JOINED,     // A player joined the room
        PLAYER_LEFT,       // A player left the room
        GAME_STARTED,      // The game has started
        ROUND_START,       // A new round has begun
        ROUND_END,         // The current round ended
        GUESS_RESULT,      // Result of a player's guess
        SCORE_UPDATE,      // Scoreboard update
        GAME_OVER,         // The game has finished
        PLAY_TRACK,        // Signal to start playing the current track
        ERROR              // Error message
    }

    private Type type;
    private String roomCode;
    private String playerName;
    private String message;        // Human-readable text
    private String trackId;        // Current track Spotify ID
    private String trackName;      // Revealed after the round
    private String artistName;     // Revealed after the round
    private String previewUrl;     // 30-second preview URL
    private Integer roundNumber;   // Current round
    private Integer totalRounds;   // Total rounds in this game
    private Integer points;        // Points awarded
    private Integer attempt;       // Guess attempt number (1..6)
    private Boolean correct;       // Whether the guess was correct
    private Map<String, Integer> scores;  // Current scoreboard
    private List<String> players;  // Players currently in the room
    private Long scheduledStartTime; // Playback start timestamp (unix ms)
}
