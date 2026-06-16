package com.guessmelody.service.impl;

import com.guessmelody.dto.response.PlaylistAnalyticsResponse;
import com.guessmelody.dto.response.PlaylistImportResponse;
import com.guessmelody.dto.response.TrackSearchResult;
import com.guessmelody.dto.response.TrackSummaryResponse;
import com.guessmelody.exception.PlaylistNotFoundException;
import com.guessmelody.exception.SpotifyApiException;
import com.guessmelody.model.entity.Playlist;
import com.guessmelody.model.entity.Track;
import com.guessmelody.repository.PlaylistRepository;
import com.guessmelody.repository.TrackRepository;
import com.guessmelody.service.PlaylistService;
import com.guessmelody.service.SpotifyPlaylistImporter;
import com.guessmelody.util.SpotifyUrlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.michaelthelin.spotify.SpotifyApi;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {

    private final SpotifyPlaylistImporter importer;
    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;

    @Override
    @Transactional
    public PlaylistImportResponse importFromUrl(String url, SpotifyApi api) {
        String playlistId = SpotifyUrlParser.extractPlaylistId(url);
        log.info("Importing playlist {} with user-provided Spotify API", playlistId);

        var existing = playlistRepository.findBySpotifyPlaylistId(playlistId);
        if (existing.isPresent()) {
            log.info("Playlist {} already imported, returning existing", playlistId);
            return mapToImportResponse(existing.get());
        }

        Playlist playlist = importer.importPlaylist(api, playlistId);

        playlist.getTracks().forEach(track -> track.setPlaylist(playlist));

        Playlist saved = playlistRepository.save(playlist);
        log.info("Saved playlist '{}' with {} tracks", saved.getName(), saved.getTracks().size());

        return mapToImportResponse(saved);
    }

    @Override
    @Transactional
    public PlaylistImportResponse importFromTrackUrls(List<String> trackUrls, String name, SpotifyApi api) {
        if (trackUrls == null || trackUrls.isEmpty()) {
            throw new IllegalArgumentException("No track links provided");
        }

        String playlistName = name != null && !name.isBlank() ? name.trim() : "Imported Tracks";
        Playlist playlist = importer.importFromTrackUrls(api, trackUrls, playlistName);

        Set<String> existingIds = trackRepository.findAll().stream()
                .map(Track::getSpotifyTrackId)
                .collect(Collectors.toSet());

        List<Track> uniqueTracks = playlist.getTracks().stream()
                .filter(t -> !existingIds.contains(t.getSpotifyTrackId()))
                .peek(t -> t.setPlaylist(playlist))
                .collect(Collectors.toList());

        if (uniqueTracks.isEmpty()) {
            throw new SpotifyApiException("All provided tracks are already in the pool or could not be fetched.");
        }

        playlist.setTracks(uniqueTracks);
        Playlist saved = playlistRepository.save(playlist);
        log.info("Saved manual playlist '{}' with {} tracks", saved.getName(), saved.getTracks().size());
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

    @Override
    @Transactional
    public Track addTrackToPlaylist(Long playlistId, TrackSearchResult searchResult) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistNotFoundException(String.valueOf(playlistId)));

        Track track = trackRepository.findBySpotifyTrackId(searchResult.getSpotifyTrackId())
                .orElseGet(() -> Track.builder()
                        .spotifyTrackId(searchResult.getSpotifyTrackId())
                        .name(searchResult.getName())
                        .artistName(searchResult.getArtistName())
                        .allArtistNames(searchResult.getAllArtistNames())
                        .durationMs(searchResult.getDurationMs())
                        .previewUrl(searchResult.getPreviewUrl())
                        .build());

        track.setPlaylist(playlist);
        return trackRepository.save(track);
    }

    @Override
    @Transactional
    public void clearPlaylistTracks(Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistNotFoundException(String.valueOf(playlistId)));

        playlist.getTracks().clear();
        playlistRepository.save(playlist);
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
