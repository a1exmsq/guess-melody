package com.guessmelody.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guessmelody.exception.SpotifyApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Backend service for controlling Spotify playback through the Spotify Web API.
 * Used together with the Web Playback SDK (play/pause/seek/volume).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyPlayerService {

    private final SpotifyAuthService authService;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SPOTIFY_API = "https://api.spotify.com/v1";

    public void playTrack(String deviceId, String trackId, Integer positionMs) {
        String token = requireToken();
        String body;
        try {
            body = objectMapper.writeValueAsString(java.util.Map.of(
                    "uris", java.util.List.of("spotify:track:" + trackId),
                    "position_ms", positionMs != null ? positionMs : 0
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build play request", e);
        }

        String uri = SPOTIFY_API + "/me/player/play" + (deviceId != null ? "?device_id=" + deviceId : "");
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        handle(request, "play");
    }

    public void seek(String deviceId, Integer positionMs) {
        String token = requireToken();
        String uri = SPOTIFY_API + "/me/player/seek?position_ms=" + (positionMs != null ? positionMs : 0)
                + (deviceId != null ? "&device_id=" + deviceId : "");
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        handle(request, "seek");
    }

    public Integer pause(String deviceId) {
        String token = requireToken();

        Integer progressMs = null;
        try {
            JsonNode state = getPlaybackState();
            if (state != null) {
                progressMs = state.path("progress_ms").asInt(0);
            }
        } catch (Exception e) {
            log.debug("Could not fetch playback state before pause: {}", e.getMessage());
        }

        String uri = SPOTIFY_API + "/me/player/pause" + (deviceId != null ? "?device_id=" + deviceId : "");
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        handle(request, "pause");
        return progressMs;
    }

    public void setVolume(String deviceId, Integer volume) {
        String token = requireToken();
        int vol = volume != null ? Math.max(0, Math.min(100, volume)) : 50;
        String uri = SPOTIFY_API + "/me/player/volume?volume_percent=" + vol
                + (deviceId != null ? "&device_id=" + deviceId : "");
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        handle(request, "volume");
    }

    public JsonNode getPlaybackState() {
        String token = requireToken();
        HttpRequest request = HttpRequest.newBuilder(URI.create(SPOTIFY_API + "/me/player"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() == 204 || response.body().isBlank()) return null;
        try {
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            log.warn("Could not parse playback state: {}", e.getMessage());
            return null;
        }
    }

    // ===== Internal helpers =====

    private String requireToken() {
        String token;
        try {
            token = authService.getAccessToken();
        } catch (Exception e) {
            throw new SpotifyApiException(401, "Spotify authorization failed: " + e.getMessage(), null);
        }
        if (token == null) {
            throw new SpotifyApiException(401, "Spotify not authorized", null);
        }
        return token;
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new SpotifyApiException(0, "Spotify request failed: " + e.getMessage(), null);
        }
    }

    private void handle(HttpRequest request, String operation) {
        HttpResponse<String> response = send(request);
        int code = response.statusCode();

        if (code >= 200 && code < 300) {
            return;
        }

        String body = response.body();
        log.warn("Spotify {} returned {}: {}", operation, code, body);

        if (code == 429) {
            int retryAfter = parseRetryAfter(response);
            throw new SpotifyApiException(code, "Spotify rate limit exceeded. Retry after " + retryAfter + "s", body, retryAfter);
        }

        String message = switch (code) {
            case 401 -> "Spotify session expired. Please reconnect.";
            case 403 -> "Spotify premium account is required to control playback.";
            case 404 -> extractDeviceNotFound(body);
            case 500, 502, 503 -> "Spotify is temporarily unavailable. Please try again.";
            default -> "Spotify " + operation + " failed (HTTP " + code + ").";
        };

        throw new SpotifyApiException(code, message, body);
    }

    private int parseRetryAfter(HttpResponse<String> response) {
        return response.headers().firstValue("Retry-After")
                .map(this::parseIntSafe)
                .filter(i -> i > 0)
                .orElse(2);
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String extractDeviceNotFound(String body) {
        if (body != null && body.toLowerCase().contains("device_id")) {
            return "Spotify device not found. Please refresh the player.";
        }
        return "Spotify resource not found.";
    }
}
