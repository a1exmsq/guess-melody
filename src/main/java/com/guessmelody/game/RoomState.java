package com.guessmelody.game;

import com.guessmelody.model.entity.Track;
import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state of a game room.
 */
@Data
public class RoomState {

    public enum Status {
        WAITING,   // Waiting for players
        PLAYING,   // Game in progress
        FINISHED   // Game finished
    }

    private final String code;
    private final String hostName;
    private final Set<String> players = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private final List<String> playedTrackIds = new ArrayList<>();
    private Long playlistId;
    private String playlistName;
    private List<Track> playlistTracks = new ArrayList<>();

    private Status status = Status.WAITING;
    private int currentRound = 0;
    private int totalRounds = 10;
    private Track currentTrack;
    private boolean roundActive = false;
    private final Set<String> roundWinners = ConcurrentHashMap.newKeySet();
    private final long createdAt = System.currentTimeMillis();
    private long lastActivityAt = System.currentTimeMillis();
    private Long finishedAt;

    public RoomState(String code, String hostName) {
        this.code = code;
        this.hostName = hostName;
        this.players.add(hostName);
        this.scores.put(hostName, 0);
    }

    public void addPlayer(String name) {
        players.add(name);
        scores.putIfAbsent(name, 0);
        touch();
    }

    public void removePlayer(String name) {
        players.remove(name);
        scores.remove(name);
        touch();
    }

    public void touch() {
        this.lastActivityAt = System.currentTimeMillis();
    }

    public void addPoints(String playerName, int points) {
        scores.merge(playerName, points, Integer::sum);
    }

    public boolean hasPlayed(String trackId) {
        return playedTrackIds.contains(trackId);
    }

    public void markPlayed(String trackId) {
        playedTrackIds.add(trackId);
    }

    public boolean isHost(String playerName) {
        return hostName.equals(playerName);
    }

    public boolean allPlayersGuessed() {
        return roundWinners.size() >= players.size();
    }
}
