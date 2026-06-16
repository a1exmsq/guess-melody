package com.guessmelody.service.impl;

import com.guessmelody.dto.response.TrackSearchResult;
import com.guessmelody.model.entity.Track;
import com.guessmelody.repository.TrackRepository;
import com.guessmelody.service.GameTrackPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameTrackPoolServiceImpl implements GameTrackPoolService {

    private final TrackRepository trackRepository;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public Track addTrack(TrackSearchResult searchResult) {
        var existing = trackRepository.findBySpotifyTrackId(searchResult.getSpotifyTrackId());
        if (existing.isPresent()) {
            log.info("Track {} is already in the pool", searchResult.getSpotifyTrackId());
            return existing.get();
        }

        Track track = Track.builder()
                .spotifyTrackId(searchResult.getSpotifyTrackId())
                .name(searchResult.getName())
                .artistName(searchResult.getArtistName())
                .allArtistNames(searchResult.getAllArtistNames())
                .durationMs(searchResult.getDurationMs())
                .previewUrl(searchResult.getPreviewUrl())
                .build();

        Track saved = trackRepository.save(track);
        log.info("Added track to pool: {} — {}", saved.getArtistName(), saved.getName());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Track> getAllTracks() {
        return trackRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Track getRandomTrack() {
        List<Track> tracks = trackRepository.findAll();
        if (tracks.isEmpty()) {
            throw new IllegalStateException("The track pool is empty. Add some tracks before starting a game.");
        }
        return tracks.get(random.nextInt(tracks.size()));
    }

    @Override
    @Transactional
    public void clearPool() {
        trackRepository.deleteAll();
        log.info("Track pool cleared");
    }

    @Override
    @Transactional(readOnly = true)
    public long getTrackCount() {
        return trackRepository.count();
    }
}
