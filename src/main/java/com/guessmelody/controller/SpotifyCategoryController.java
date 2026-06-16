package com.guessmelody.controller;

import com.guessmelody.service.SpotifyAuthService;
import com.guessmelody.service.SpotifyCategoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyCategoryController {

    private final SpotifyCategoryService categoryService;
    private final SpotifyAuthService authService;

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories(HttpSession session) {
        try {
            var api = authService.requireUserApi(session);
            var categories = categoryService.getCategories(api);
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
    public ResponseEntity<?> getCategoryPlaylists(@PathVariable String id, HttpSession session) {
        try {
            var api = authService.requireUserApi(session);
            var playlists = categoryService.getCategoryPlaylists(api, id);
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
    public ResponseEntity<?> importRandomPlaylist(HttpSession session) {
        try {
            var api = authService.requireUserApi(session);
            var playlist = categoryService.importRandomPlaylist(api);
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
