package com.guessmelody.controller;

import com.guessmelody.exception.SpotifyApiException;
import com.guessmelody.service.SpotifyPlayerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for Spotify Web Playback SDK operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyPlayerController {

    private final SpotifyPlayerService playerService;

    @PostMapping("/play")
    public ResponseEntity<?> play(@RequestBody PlayRequest req) {
        try {
            playerService.playTrack(req.getDeviceId(), req.getTrackId(), req.getPositionMs());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return handleError("play", e);
        }
    }

    @PostMapping("/seek")
    public ResponseEntity<?> seek(@RequestBody SeekRequest req) {
        try {
            playerService.seek(req.getDeviceId(), req.getPositionMs());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return handleError("seek", e);
        }
    }

    @PostMapping("/pause")
    public ResponseEntity<?> pause(@RequestBody PauseRequest req) {
        try {
            Integer progressMs = playerService.pause(req.getDeviceId());
            var response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("progressMs", progressMs);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleError("pause", e);
        }
    }

    @PostMapping("/volume")
    public ResponseEntity<?> volume(@RequestBody VolumeRequest req) {
        try {
            playerService.setVolume(req.getDeviceId(), req.getVolume());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return handleError("volume", e);
        }
    }

    @GetMapping("/playback")
    public ResponseEntity<?> getPlayback() {
        try {
            var state = playerService.getPlaybackState();
            if (state == null) {
                return ResponseEntity.ok(Map.of("playing", false));
            }
            return ResponseEntity.ok(Map.of(
                    "playing", state.path("is_playing").asBoolean(false),
                    "progressMs", state.path("progress_ms").asInt(0),
                    "durationMs", state.path("item").path("duration_ms").asInt(0),
                    "trackName", state.path("item").path("name").asText(""),
                    "artistName", state.path("item").path("artists").get(0).path("name").asText("")
            ));
        } catch (SpotifyApiException e) {
            return ResponseEntity.status(mapStatus(e.getStatusCode()))
                    .body(errorBody(e));
        } catch (Exception e) {
            log.error("Playback state error", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<?> handleError(String operation, Exception e) {
        if (e instanceof SpotifyApiException ex) {
            log.warn("Spotify {} failed: HTTP {} - {}", operation, ex.getStatusCode(), ex.getMessage());
            return ResponseEntity.status(mapStatus(ex.getStatusCode())).body(errorBody(ex));
        }
        log.error("Spotify {} error", operation, e);
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    private Map<String, Object> errorBody(SpotifyApiException e) {
        var body = new HashMap<String, Object>();
        body.put("success", false);
        body.put("error", e.getMessage());
        body.put("statusCode", e.getStatusCode());
        if (e.getRetryAfterSeconds() > 0) {
            body.put("retryAfter", e.getRetryAfterSeconds());
        }
        return body;
    }

    private int mapStatus(int spotifyStatusCode) {
        return switch (spotifyStatusCode) {
            case 401 -> 401;
            case 403 -> 403;
            case 429 -> 429;
            case 404 -> 404;
            case 500, 502, 503 -> 503;
            default -> 400;
        };
    }

    @Data
    public static class PlayRequest {
        private String deviceId;
        private String trackId;
        private Integer positionMs;
    }

    @Data
    public static class PauseRequest {
        private String deviceId;
    }

    @Data
    public static class SeekRequest {
        private String deviceId;
        private Integer positionMs;
    }

    @Data
    public static class VolumeRequest {
        private String deviceId;
        private Integer volume;
    }
}
