package com.guessmelody.service;

import com.guessmelody.dto.GameMessage;
import com.guessmelody.game.RoomState;
import com.guessmelody.model.entity.Playlist;
import com.guessmelody.model.entity.Track;
import com.guessmelody.repository.PlaylistRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameTrackPoolService trackPoolService;
    private final PlaylistRepository playlistRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecureRandom random = new SecureRandom();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();

    private static final int[] POINTS = {100, 80, 60, 40, 20, 10};
    private static final int MAX_ATTEMPTS = 6;
    private static final long ROOM_TTL_MINUTES = 30;
    private static final long FINISHED_ROOM_TTL_MINUTES = 5;

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::cleanupRooms, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public RoomState createRoom(String hostName) {
        String code = generateRoomCode();
        RoomState room = new RoomState(code, hostName);
        rooms.put(code, room);
        log.info("Room {} created by host {}", code, hostName);
        return room;
    }

    @Transactional(readOnly = true)
    public RoomState createRoom(String hostName, Long playlistId) {
        if (playlistId == null) {
            return createRoom(hostName);
        }

        String code = generateRoomCode();
        RoomState room = new RoomState(code, hostName);
        Playlist playlist = playlistRepository.findWithTracksById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));
        List<Track> tracks = playlist.getTracks().stream()
                .filter(t -> t.getSpotifyTrackId() != null)
                .toList();

        if (tracks.isEmpty()) {
            throw new IllegalStateException("Selected playlist has no tracks");
        }

        room.setPlaylistId(playlist.getId());
        room.setPlaylistName(playlist.getName());
        room.setPlaylistTracks(new ArrayList<>(tracks));
        room.setTotalRounds(Math.min(10, tracks.size()));
        rooms.put(code, room);
        log.info("Created room {} host={} playlist={}", code, hostName, room.getPlaylistName());
        return room;
    }

    public RoomState joinRoom(String code, String playerName) {
        RoomState room = rooms.get(code.toUpperCase());
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        if (room.getStatus() != RoomState.Status.WAITING) {
            throw new IllegalStateException("Game already started");
        }
        room.addPlayer(playerName);
        log.info("Player {} joined room {}", playerName, code);
        return room;
    }

    public void leaveRoom(String code, String playerName) {
        RoomState room = rooms.get(code.toUpperCase());
        if (room == null) return;

        room.removePlayer(playerName);
        log.info("Player {} left room {}", playerName, code);

        broadcast(code, GameMessage.builder()
                .type(GameMessage.Type.PLAYER_LEFT)
                .roomCode(code)
                .playerName(playerName)
                .scores(new HashMap<>(room.getScores()))
                .players(new ArrayList<>(room.getPlayers()))
                .build());

        if (room.getPlayers().isEmpty()) {
            rooms.remove(code.toUpperCase());
            log.info("Room {} removed (empty)", code);
        }
    }

    public RoomState getRoom(String code) {
        return rooms.get(code.toUpperCase());
    }

    public void removeRoom(String code) {
        rooms.remove(code.toUpperCase());
    }

    public void startGame(String code, String playerName) {
        RoomState room = getRoomOrThrow(code);
        if (!room.isHost(playerName)) {
            throw new IllegalStateException("Only host can start the game");
        }
        if (room.getPlayers().size() < 1) {
            throw new IllegalStateException("At least 1 player required");
        }
        room.setStatus(RoomState.Status.PLAYING);
        room.setCurrentRound(0);
        room.getPlayedTrackIds().clear();
        room.touch();

        broadcast(code, GameMessage.builder()
                .type(GameMessage.Type.GAME_STARTED)
                .roomCode(code)
                .message("Game started! Total rounds: " + room.getTotalRounds())
                .totalRounds(room.getTotalRounds())
                .scores(new HashMap<>(room.getScores()))
                .players(new ArrayList<>(room.getPlayers()))
                .build());

        scheduleNextRound(code, 2000);
    }

    public void startRound(String code) {
        RoomState room = getRoomOrThrow(code);
        if (room.getStatus() != RoomState.Status.PLAYING) return;

        if (room.getCurrentRound() >= room.getTotalRounds()) {
            endGame(code);
            return;
        }

        room.setCurrentRound(room.getCurrentRound() + 1);
        room.setRoundActive(true);
        room.getRoundWinners().clear();
        room.touch();

        Track track = pickRandomTrack(room);
        if (track == null) {
            broadcast(code, GameMessage.builder()
                    .type(GameMessage.Type.ERROR)
                    .message("Failed to pick a track. Game over.")
                    .build());
            endGame(code);
            return;
        }

        room.setCurrentTrack(track);
        room.markPlayed(track.getSpotifyTrackId());

        log.info("Round {}/{} in room {}: {} — {}",
                room.getCurrentRound(), room.getTotalRounds(), code,
                track.getArtistName(), track.getName());

        long scheduledStartTime = System.currentTimeMillis() + 2000L;

        broadcast(code, GameMessage.builder()
                .type(GameMessage.Type.ROUND_START)
                .roomCode(code)
                .roundNumber(room.getCurrentRound())
                .totalRounds(room.getTotalRounds())
                .trackId(track.getSpotifyTrackId())
                .previewUrl(track.getPreviewUrl())
                .scheduledStartTime(scheduledStartTime)
                .message("Round " + room.getCurrentRound() + "/" + room.getTotalRounds() + "! Guess the track!")
                .scores(new HashMap<>(room.getScores()))
                .build());

        startRoundTimer(code, 45000);
    }

    public void playTrackRequest(String code, String playerName) {
        RoomState room = getRoomOrThrow(code);
        if (!room.isHost(playerName)) {
            throw new IllegalStateException("Only host can start playback");
        }
        if (!room.isRoundActive() || room.getCurrentTrack() == null) {
            return;
        }
        Track track = room.getCurrentTrack();
        long scheduledStartTime = System.currentTimeMillis() + 1500L;

        broadcast(code, GameMessage.builder()
                .type(GameMessage.Type.PLAY_TRACK)
                .roomCode(code)
                .trackId(track.getSpotifyTrackId())
                .previewUrl(track.getPreviewUrl())
                .scheduledStartTime(scheduledStartTime)
                .message("Playing track")
                .build());
    }

    public void submitGuess(String code, String playerName, String guess, int attempt) {
        RoomState room = getRoomOrThrow(code);
        if (!room.isRoundActive()) return;
        if (room.getRoundWinners().contains(playerName)) return;

        Track track = room.getCurrentTrack();
        if (track == null) return;

        boolean correct = isCorrectGuess(guess, track);

        if (correct) {
            int points = (attempt >= 1 && attempt <= MAX_ATTEMPTS)
                    ? POINTS[attempt - 1]
                    : POINTS[POINTS.length - 1];
            room.addPoints(playerName, points);
            room.getRoundWinners().add(playerName);

            broadcast(code, GameMessage.builder()
                    .type(GameMessage.Type.GUESS_RESULT)
                    .roomCode(code)
                    .playerName(playerName)
                    .correct(true)
                    .points(points)
                    .attempt(attempt)
                    .message("✅ " + playerName + " guessed! +" + points + " points")
                    .scores(new HashMap<>(room.getScores()))
                    .build());

            log.info("{} guessed in room {} (attempt {}, +{} points)",
                    playerName, code, attempt, points);

            if (room.allPlayersGuessed() || attempt >= MAX_ATTEMPTS) {
                scheduleRoundEnd(code, 1000);
            }
        } else {
            broadcast(code, GameMessage.builder()
                    .type(GameMessage.Type.GUESS_RESULT)
                    .roomCode(code)
                    .playerName(playerName)
                    .correct(false)
                    .attempt(attempt)
                    .message("❌ " + playerName + " missed (attempt " + attempt + "/" + MAX_ATTEMPTS + ")")
                    .build());
        }
    }

    public void endRound(String code) {
        RoomState room = getRoomOrThrow(code);
        if (!room.isRoundActive()) return;

        room.setRoundActive(false);
        Track track = room.getCurrentTrack();

        String answer = track != null ? track.getArtistName() + " — " + track.getName() : "???";

        broadcast(code, GameMessage.builder()
                .type(GameMessage.Type.ROUND_END)
                .roomCode(code)
                .roundNumber(room.getCurrentRound())
                .trackName(track != null ? track.getName() : null)
                .artistName(track != null ? track.getArtistName() : null)
                .message("Round over! Answer: " + answer)
                .scores(new HashMap<>(room.getScores()))
                .build());

        if (room.getCurrentRound() < room.getTotalRounds()) {
            scheduleNextRound(code, 5000);
        } else {
            schedule(() -> endGame(code), 5000);
        }
    }

    public void endGame(String code) {
        RoomState room = getRoomOrThrow(code);
        room.setStatus(RoomState.Status.FINISHED);
        room.setFinishedAt(System.currentTimeMillis());

        String winner = room.getScores().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Nobody");

        broadcast(code, GameMessage.builder()
                .type(GameMessage.Type.GAME_OVER)
                .roomCode(code)
                .message("🏆 Game over! Winner: " + winner)
                .scores(new HashMap<>(room.getScores()))
                .players(new ArrayList<>(room.getPlayers()))
                .build());

        log.info("Game in room {} finished. Winner: {}", code, winner);
    }

    public void startRoundTimer(String code, int durationMs) {
        schedule(() -> {
            RoomState room = getRoom(code);
            if (room != null && room.isRoundActive()) {
                endRound(code);
            }
        }, durationMs);
    }

    private Track pickRandomTrack(RoomState room) {
        List<Track> allTracks = room.getPlaylistTracks() != null && !room.getPlaylistTracks().isEmpty()
                ? room.getPlaylistTracks()
                : trackPoolService.getAllTracks();
        if (allTracks.isEmpty()) return null;

        List<Track> available = allTracks.stream()
                .filter(t -> !room.hasPlayed(t.getSpotifyTrackId()))
                .toList();

        if (available.isEmpty()) {
            room.getPlayedTrackIds().clear();
            available = allTracks;
        }

        return available.get(random.nextInt(available.size()));
    }

    private boolean isCorrectGuess(String guess, Track track) {
        if (guess == null || guess.isBlank()) return false;
        List<String> guessTokens = tokenize(guess);
        if (guessTokens.isEmpty()) return false;

        List<String> nameTokens = tokenize(normalizeTitle(track.getName()));
        List<String> artistTokens = tokenize(track.getArtistName());

        return containsTokensInOrder(guessTokens, nameTokens)
                || containsTokensInOrder(guessTokens, artistTokens);
    }

    private static String normalizeTitle(String title) {
        return title.replaceAll("(?i)\\s*\\([^)]*\\)", "")
                .replaceAll("(?i)\\s*\\[[^\\]]*\\]", "")
                .replaceAll("(?i)\\s+-\\s+.*", "")
                .trim();
    }

    private static List<String> tokenize(String input) {
        return Arrays.stream(input.toLowerCase().split("[^a-z0-9]+"))
                .filter(t -> !t.isBlank())
                .toList();
    }

    private static boolean containsTokensInOrder(List<String> container, List<String> target) {
        if (target.isEmpty()) return false;
        int i = 0;
        for (String token : container) {
            if (token.equals(target.get(i))) {
                i++;
                if (i == target.size()) return true;
            }
        }
        return false;
    }

    private RoomState getRoomOrThrow(String code) {
        RoomState room = rooms.get(code.toUpperCase());
        if (room == null) throw new IllegalArgumentException("Room not found");
        return room;
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String code = sb.toString();
        return rooms.containsKey(code) ? generateRoomCode() : code;
    }

    private void broadcast(String roomCode, GameMessage message) {
        messagingTemplate.convertAndSend("/topic/room/" + roomCode.toUpperCase(), message);
    }

    private void scheduleNextRound(String code, int delayMs) {
        schedule(() -> startRound(code), delayMs);
    }

    private void scheduleRoundEnd(String code, int delayMs) {
        schedule(() -> endRound(code), delayMs);
    }

    private void schedule(Runnable task, int delayMs) {
        scheduler.schedule(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Scheduler task error", e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private void cleanupRooms() {
        long now = System.currentTimeMillis();
        rooms.entrySet().removeIf(entry -> {
            RoomState room = entry.getValue();
            boolean empty = room.getPlayers().isEmpty();
            boolean stale = now - room.getLastActivityAt() > TimeUnit.MINUTES.toMillis(ROOM_TTL_MINUTES);
            boolean finishedOld = room.getStatus() == RoomState.Status.FINISHED
                    && room.getFinishedAt() != null
                    && now - room.getFinishedAt() > TimeUnit.MINUTES.toMillis(FINISHED_ROOM_TTL_MINUTES);
            if (empty || stale || finishedOld) {
                log.info("Cleaning up room {} (empty={}, stale={}, finishedOld={})",
                        entry.getKey(), empty, stale, finishedOld);
                return true;
            }
            return false;
        });
    }
}
