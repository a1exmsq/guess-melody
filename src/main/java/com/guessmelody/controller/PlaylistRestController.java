package com.guessmelody.controller;

import com.guessmelody.dto.request.PlaylistImportRequest;
import com.guessmelody.dto.request.TrackListImportRequest;
import com.guessmelody.dto.response.PlaylistAnalyticsResponse;
import com.guessmelody.dto.response.PlaylistImportResponse;
import com.guessmelody.exception.SpotifyApiException;
import com.guessmelody.service.PlaylistService;
import com.guessmelody.service.SpotifyAuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.michaelthelin.spotify.SpotifyApi;

@Slf4j
@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistRestController {

    private final PlaylistService playlistService;
    private final SpotifyAuthService authService;

    @PostMapping("/import")
    public ResponseEntity<PlaylistImportResponse> importPlaylist(
            @Valid @RequestBody PlaylistImportRequest request,
            HttpSession session) {
        if (authService.isUserAuthorized(session)) {
            try {
                SpotifyApi userApi = authService.requireUserApi(session);
                PlaylistImportResponse response = playlistService.importFromUrl(request.getUrl(), userApi);
                return ResponseEntity.ok(response);
            } catch (SpotifyApiException e) {
                int status = e.getStatusCode();
                if (status == 401 || status == 403 || status == 404) {
                    log.warn("User token failed to import playlist (HTTP {}), trying backend token", status);
                    try {
                        PlaylistImportResponse response = playlistService.importFromUrl(request.getUrl(), authService.requireBackendApi());
                        return ResponseEntity.ok(response);
                    } catch (SpotifyApiException backendEx) {
                        log.warn("Backend token also failed: {}", backendEx.getMessage());
                    }
                }
                throw e;
            }
        }
        PlaylistImportResponse response = playlistService.importFromUrl(request.getUrl(), authService.requireBackendApi());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/import-tracks")
    public ResponseEntity<PlaylistImportResponse> importTrackList(
            @Valid @RequestBody TrackListImportRequest request,
            HttpSession session) {
        if (authService.isUserAuthorized(session)) {
            try {
                SpotifyApi userApi = authService.requireUserApi(session);
                PlaylistImportResponse response = playlistService.importFromTrackUrls(
                        request.getTrackUrls(), request.getName(), userApi);
                return ResponseEntity.ok(response);
            } catch (SpotifyApiException e) {
                int status = e.getStatusCode();
                if (status == 401 || status == 403 || status == 404) {
                    log.warn("User token failed to import track list (HTTP {}), trying backend token", status);
                    PlaylistImportResponse response = playlistService.importFromTrackUrls(
                            request.getTrackUrls(), request.getName(), authService.requireBackendApi());
                    return ResponseEntity.ok(response);
                }
                throw e;
            }
        }
        PlaylistImportResponse response = playlistService.importFromTrackUrls(
                request.getTrackUrls(), request.getName(), authService.requireBackendApi());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<PlaylistAnalyticsResponse> getAnalytics(@PathVariable Long id) {
        PlaylistAnalyticsResponse analytics = playlistService.getAnalytics(id);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/{id}/tracks")
    public ResponseEntity<?> getPlaylistTracks(@PathVariable Long id) {
        var playlist = playlistService.findById(id);
        var tracks = playlist.getTracks().stream()
                .map(t -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", t.getId());
                    map.put("spotifyTrackId", t.getSpotifyTrackId());
                    map.put("name", t.getName());
                    map.put("artistName", t.getArtistName());
                    map.put("allArtistNames", t.getAllArtistNames() != null ? t.getAllArtistNames() : "");
                    map.put("previewUrl", t.getPreviewUrl() != null ? t.getPreviewUrl() : "");
                    map.put("durationMs", t.getDurationMs());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(tracks);
    }

    @ExceptionHandler(SpotifyApiException.class)
    public ResponseEntity<?> handleSpotifyApiException(SpotifyApiException e) {
        int status = e.getStatusCode() > 0 ? e.getStatusCode() : 400;
        if (status < 400 || status >= 600) {
            status = 400;
        }
        return ResponseEntity.status(status).body(java.util.Map.of("error", e.getMessage()));
    }
}
