package com.guessmelody.service.impl;

import com.guessmelody.dto.response.PlaylistAnalyticsResponse;
import com.guessmelody.dto.response.PlaylistImportResponse;
import com.guessmelody.dto.response.TrackSummaryResponse;
import com.guessmelody.exception.PlaylistNotFoundException;
import com.guessmelody.model.entity.Playlist;
import com.guessmelody.model.entity.Track;
import com.guessmelody.repository.PlaylistRepository;
import com.guessmelody.service.PlaylistService;
import com.guessmelody.service.SpotifyAuthService;
import com.guessmelody.service.SpotifyPlaylistImporter;
import com.guessmelody.util.SpotifyUrlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Playlist import and analytics backed by user Spotify OAuth.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {

    private final SpotifyAuthService authService;
    private final SpotifyPlaylistImporter importer;
    private final PlaylistRepository playlistRepository;

    @Override
    @Transactional
    public PlaylistImportResponse importFromUrl(String url) {
        String playlistId = SpotifyUrlParser.extractPlaylistId(url);
        log.info("Importing playlist {} (OAuth: {})", playlistId, authService.isAuthorized());

        var existing = playlistRepository.findBySpotifyPlaylistId(playlistId);
        if (existing.isPresent()) {
            log.info("Playlist {} already imported, returning existing", playlistId);
            return mapToImportResponse(existing.get());
        }

        var userApi = authService.requireUserApi();
        Playlist playlist = importer.importPlaylist(userApi, playlistId);

        playlist.getTracks().forEach(track -> track.setPlaylist(playlist));

        Playlist saved = playlistRepository.save(playlist);
        log.info("Saved playlist '{}' with {} tracks", saved.getName(), saved.getTracks().size());

        return mapToImportResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PlaylistAnalyticsResponse getAnalytics(Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistNotFoundException(String.valueOf(playlistId)));

        List<Track> tracks = playlist.getTracks();
        long russianCount = tracks.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsRussian()))
                .count();

        Map<String, Long> genreCounts = tracks.stream()
                .map(Track::getGenres)
                .filter(g -> g != null && !g.isBlank())
                .flatMap(g -> Arrays.stream(g.split(",")))
                .map(String::trim)
                .filter(g -> !g.isEmpty())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));

        Map<String, Long> topGenres = genreCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        int total = tracks.size();
        int russian = (int) russianCount;
        int foreign = total - russian;

        return PlaylistAnalyticsResponse.builder()
                .totalTracks(total)
                .russianTracks(russian)
                .foreignTracks(foreign)
                .russianPercent(total == 0 ? 0 : Math.round(russian * 100.0 / total))
                .foreignPercent(total == 0 ? 0 : Math.round(foreign * 100.0 / total))
                .topGenres(topGenres)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Playlist findById(Long id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> new PlaylistNotFoundException(String.valueOf(id)));
    }

    private PlaylistImportResponse mapToImportResponse(Playlist playlist) {
        List<TrackSummaryResponse> trackSummaries = playlist.getTracks().stream()
                .limit(10)
                .map(t -> TrackSummaryResponse.builder()
                        .spotifyTrackId(t.getSpotifyTrackId())
                        .name(t.getName())
                        .artistName(t.getArtistName())
                        .durationMs(t.getDurationMs())
                        .build())
                .collect(Collectors.toList());

        return PlaylistImportResponse.builder()
                .id(playlist.getId())
                .spotifyPlaylistId(playlist.getSpotifyPlaylistId())
                .name(playlist.getName())
                .trackCount(playlist.getTracks().size())
                .tracks(trackSummaries)
                .createdAt(playlist.getCreatedAt())
                .build();
    }
}
