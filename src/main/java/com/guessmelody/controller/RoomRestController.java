package com.guessmelody.controller;

import com.guessmelody.game.RoomState;
import com.guessmelody.service.GameService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for creating and inspecting game rooms.
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomRestController {

    private final GameService gameService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRoom(@RequestBody CreateRoomRequest req) {
        RoomState room = gameService.createRoom(req.getPlayerName(), req.getPlaylistId());
        return ResponseEntity.ok(Map.of(
                "code", room.getCode(),
                "hostName", room.getHostName(),
                "players", List.copyOf(room.getPlayers()),
                "scores", Map.copyOf(room.getScores()),
                "playlistId", room.getPlaylistId() != null ? room.getPlaylistId() : "",
                "playlistName", room.getPlaylistName() != null ? room.getPlaylistName() : "",
                "trackCount", room.getPlaylistTracks() != null ? room.getPlaylistTracks().size() : 0
        ));
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> getRoom(@PathVariable String code) {
        RoomState room = gameService.getRoom(code);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "code", room.getCode(),
                "hostName", room.getHostName(),
                "players", List.copyOf(room.getPlayers()),
                "scores", Map.copyOf(room.getScores()),
                "status", room.getStatus().name(),
                "playlistId", room.getPlaylistId() != null ? room.getPlaylistId() : "",
                "playlistName", room.getPlaylistName() != null ? room.getPlaylistName() : "",
                "trackCount", room.getPlaylistTracks() != null ? room.getPlaylistTracks().size() : 0
        ));
    }

    @Data
    public static class CreateRoomRequest {
        private String playerName;
        private Long playlistId;
    }
}
