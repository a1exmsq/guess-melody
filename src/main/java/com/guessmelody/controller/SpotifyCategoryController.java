package com.guessmelody.controller;

import com.guessmelody.service.SpotifyCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for Spotify categories and random playlist import.
 * Uses Client Credentials, so no user OAuth is required.
 */
@Slf4j
@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyCategoryController {

    private final SpotifyCategoryService categoryService;

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        try {
            var categories = categoryService.getCategories();
            var result = categories.stream()
                    .map(c -> Map.of(
                            "id", c.getId(),
                            "name", c.getName()
                    ))
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to fetch Spotify categories", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/categories/{id}/playlists")
    public ResponseEntity<?> getCategoryPlaylists(@PathVariable String id) {
        try {
            var playlists = categoryService.getCategoryPlaylists(id);
            var result = playlists.stream()
                    .map(p -> Map.of(
                            "id", p.getId(),
                            "name", p.getName(),
                            "image", p.getImages() != null && p.getImages().length > 0 ? p.getImages()[0].getUrl() : ""
                    ))
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to fetch playlists for category {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/random-playlist")
    public ResponseEntity<?> importRandomPlaylist() {
        try {
            var playlist = categoryService.importRandomPlaylist();
            return ResponseEntity.ok(Map.of(
                    "id", playlist.getId(),
                    "name", playlist.getName(),
                    "trackCount", playlist.getTracks().size(),
                    "spotifyPlaylistId", playlist.getSpotifyPlaylistId()
            ));
        } catch (Exception e) {
            log.error("Failed to import a random playlist", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
