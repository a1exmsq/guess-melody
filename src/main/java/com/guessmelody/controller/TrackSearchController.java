package com.guessmelody.controller;

import com.guessmelody.dto.response.TrackSearchResult;
import com.guessmelody.model.entity.Track;
import com.guessmelody.service.GameTrackPoolService;
import com.guessmelody.service.TrackSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for Spotify track search and the in-memory game track pool.
 */
@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor
public class TrackSearchController {

    private final TrackSearchService trackSearchService;
    private final GameTrackPoolService gameTrackPoolService;

    @GetMapping("/search")
    public ResponseEntity<List<TrackSearchResult>> search(@RequestParam String q) {
        List<TrackSearchResult> results = trackSearchService.search(q);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/pool")
    public ResponseEntity<Track> addToPool(@RequestBody TrackSearchResult track) {
        Track saved = gameTrackPoolService.addTrack(track);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/pool")
    public ResponseEntity<List<Track>> getPool() {
        List<Track> tracks = gameTrackPoolService.getAllTracks();
        return ResponseEntity.ok(tracks);
    }

    @GetMapping("/pool/count")
    public ResponseEntity<Long> getPoolCount() {
        return ResponseEntity.ok(gameTrackPoolService.getTrackCount());
    }

    @DeleteMapping("/pool")
    public ResponseEntity<Void> clearPool() {
        gameTrackPoolService.clearPool();
        return ResponseEntity.ok().build();
    }
}
