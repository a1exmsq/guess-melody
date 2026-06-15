package com.guessmelody.controller;

import com.guessmelody.dto.request.PlaylistImportRequest;
import com.guessmelody.dto.response.PlaylistAnalyticsResponse;
import com.guessmelody.dto.response.PlaylistImportResponse;
import com.guessmelody.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistRestController {

    private final PlaylistService playlistService;

    @PostMapping("/import")
    public ResponseEntity<PlaylistImportResponse> importPlaylist(
            @Valid @RequestBody PlaylistImportRequest request) {
        PlaylistImportResponse response = playlistService.importFromUrl(request.getUrl());
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
                .map(t -> java.util.Map.of(
                        "id", t.getId(),
                        "spotifyTrackId", t.getSpotifyTrackId(),
                        "name", t.getName(),
                        "artistName", t.getArtistName(),
                        "previewUrl", t.getPreviewUrl() != null ? t.getPreviewUrl() : "",
                        "durationMs", t.getDurationMs()
                ))
                .toList();
        return ResponseEntity.ok(tracks);
    }
}
