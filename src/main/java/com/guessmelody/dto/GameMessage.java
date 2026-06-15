package com.guessmelody.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage {

    public enum Type {
        ROOM_CREATED,
        PLAYER_JOINED,
        PLAYER_LEFT,
        GAME_STARTED,
        ROUND_START,
        ROUND_END,
        GUESS_RESULT,
        SCORE_UPDATE,
        GAME_OVER,
        PLAY_TRACK,
        ERROR
    }

    private Type type;
    private String roomCode;
    private String playerName;
    private String message;
    private String trackId;
    private String trackName;
    private String artistName;
    private String previewUrl;
    private Integer roundNumber;
    private Integer totalRounds;
    private Integer points;
    private Integer attempt;
    private Boolean correct;
    private Map<String, Integer> scores;
    private List<String> players;
    private Long scheduledStartTime;
}
