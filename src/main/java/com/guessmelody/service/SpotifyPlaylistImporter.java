package com.guessmelody.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guessmelody.exception.SpotifyApiException;
import com.guessmelody.model.entity.Playlist;
import com.guessmelody.model.entity.Track;
import com.guessmelody.util.CyrillicDetector;
import com.neovisionaries.i18n.CountryCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports Spotify playlists using any authenticated SpotifyApi instance.
 * Works with both Client Credentials and user OAuth tokens.
 */
@Slf4j
@Component
public class SpotifyPlaylistImporter {

    private static final String SPOTIFY_API_BASE = "https://api.spotify.com/v1";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Playlist importPlaylist(SpotifyApi api, String playlistId) {
        try {
            log.info("Requesting playlist {} from Spotify...", playlistId);

            CountryCode userMarket = resolveUserMarket(api);
            var playlistInfo = loadPlaylistInfo(api, playlistId, userMarket);

            log.info("Spotify response: name='{}', tracks.total={}, tracks.limit={}",
                    playlistInfo.getName(),
                    playlistInfo.getTracks() != null ? playlistInfo.getTracks().getTotal() : "null",
                    playlistInfo.getTracks() != null ? playlistInfo.getTracks().getLimit() : "null");

            List<Track> tracks = fetchAllTracks(api, playlistId, playlistInfo.getTracks(), userMarket);

            if (tracks.isEmpty()) {
                log.info("No tracks from getPlaylist(), trying getPlaylistItems()...");
                tracks = fetchTracksViaItems(api, playlistId, userMarket);
            }

            if (tracks.isEmpty()) {
                throw new SpotifyApiException(
                        "Spotify returned the playlist but no importable tracks. " +
                        "Make sure the playlist contains regular Spotify tracks, not only local files or podcasts."
                );
            }

            return Playlist.builder()
                    .spotifyPlaylistId(playlistId)
                    .name(playlistInfo.getName())
                    .tracks(tracks)
                    .build();

        } catch (se.michaelthelin.spotify.exceptions.detailed.NotFoundException nfe) {
            log.warn("getPlaylist() returned 404, trying getPlaylistItems() directly...");
            try {
                List<Track> tracks = fetchTracksViaItems(api, playlistId, resolveUserMarket(api));
                if (!tracks.isEmpty()) {
                    return Playlist.builder()
                            .spotifyPlaylistId(playlistId)
                            .name("Imported Playlist")
                            .tracks(tracks)
                            .build();
                }
            } catch (Exception fallbackEx) {
                log.error("getPlaylistItems() also failed: {}", fallbackEx.getMessage());
            }
            throw new SpotifyApiException("Playlist not found (404). It may be private or unavailable in your region.", nfe);
        } catch (Exception e) {
            log.error("Failed to import playlist {}", playlistId, e);
            throw new SpotifyApiException("Could not import playlist: " + e.getMessage(), e);
        }
    }

    private se.michaelthelin.spotify.model_objects.specification.Playlist loadPlaylistInfo(
            SpotifyApi api, String playlistId, CountryCode userMarket) throws Exception {
        Exception lastError = null;

        for (CountryCode market : marketsToTry(userMarket)) {
            try {
                if (market == null) {
                    return api.getPlaylist(playlistId)
                            .additionalTypes("track")
                            .build()
                            .execute();
                }
                return api.getPlaylist(playlistId)
                        .market(market)
                        .additionalTypes("track")
                        .build()
                        .execute();
            } catch (Exception e) {
                lastError = e;
                log.warn("getPlaylist market={} failed: {}", market, e.getMessage());
            }
        }

        throw lastError != null ? lastError : new IllegalStateException("Could not load playlist");
    }

    private List<Track> fetchTracksViaItems(SpotifyApi api, String playlistId, CountryCode userMarket) throws Exception {
        for (CountryCode market : marketsToTry(userMarket)) {
            List<Track> tracks = fetchTracksViaItemsForMarket(api, playlistId, market);
            if (!tracks.isEmpty()) {
                return tracks;
            }
        }
        for (CountryCode market : marketsToTry(userMarket)) {
            List<Track> tracks = fetchTracksViaItemsEndpoint(api, playlistId, market);
            if (!tracks.isEmpty()) {
                return tracks;
            }
        }
        return List.of();
    }

    private List<Track> fetchTracksViaItemsForMarket(SpotifyApi api, String playlistId, CountryCode market) throws Exception {
        List<Track> result = new ArrayList<>();
        Map<String, Artist> artistCache = new HashMap<>();
        int offset = 0;
        int limit = 100;
        boolean hasMore = true;

        while (hasMore) {
            try {
                var request = api.getPlaylistsItems(playlistId)
                        .additionalTypes("track")
                        .limit(limit)
                        .offset(offset);
                if (market != null) {
                    request.market(market);
                }
                Paging<PlaylistTrack> paging = request.build().execute();

                PlaylistTrack[] items = paging.getItems();
                if (items == null || items.length == 0) break;

                for (PlaylistTrack item : items) {
                    convertAndAdd(api, item, artistCache, result);
                }

                offset += limit;
                hasMore = paging.getNext() != null;

            } catch (se.michaelthelin.spotify.exceptions.detailed.ForbiddenException fe) {
                log.warn("Playlist page access restricted (403). Returning {} tracks.", result.size());
                break;
            } catch (Exception e) {
                log.warn("Failed to load offset={}: {}", offset, e.getMessage());
                break;
            }
        }

        log.info("fetchTracksViaItems loaded {} tracks", result.size());
        return result;
    }

    private List<Track> fetchTracksViaItemsEndpoint(SpotifyApi api, String playlistId, CountryCode market) throws Exception {
        List<Track> result = new ArrayList<>();
        Map<String, Artist> artistCache = new HashMap<>();
        int offset = 0;
        int limit = 100;
        boolean hasMore = true;

        while (hasMore) {
            URI uri = URI.create(SPOTIFY_API_BASE + "/playlists/" + encode(playlistId) + "/items"
                    + "?limit=" + limit
                    + "&offset=" + offset
                    + "&additional_types=track"
                    + (market != null ? "&market=" + encode(market.getAlpha2()) : ""));

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Authorization", "Bearer " + api.getAccessToken())
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 403) {
                log.warn("Spotify /items endpoint blocked playlist={}, market={} (403): {}",
                        playlistId, market, response.body());
                break;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new SpotifyApiException("Spotify /items returned HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode items = root.path("items");
            if (!items.isArray() || items.isEmpty()) {
                break;
            }

            for (JsonNode item : items) {
                convertAndAddJson(api, item, artistCache, result);
            }

            offset += limit;
            hasMore = !root.path("next").isNull() && !root.path("next").asText("").isBlank();
        }

        log.info("Spotify /items endpoint loaded {} tracks for playlist={}, market={}", result.size(), playlistId, market);
        return result;
    }

    private List<Track> fetchAllTracks(SpotifyApi api, String playlistId,
                                       Paging<PlaylistTrack> initialTracks,
                                       CountryCode userMarket) throws Exception {
        List<Track> result = new ArrayList<>();
        Map<String, Artist> artistCache = new HashMap<>();

        if (initialTracks != null && initialTracks.getItems() != null) {
            for (PlaylistTrack item : initialTracks.getItems()) {
                convertAndAdd(api, item, artistCache, result);
            }
            log.info("Got {} tracks from getPlaylist()", result.size());
        }

        if (initialTracks != null && initialTracks.getNext() != null) {
            int offset = initialTracks.getItems().length;
            int limit = 100;
            boolean hasMore = true;

            while (hasMore) {
                try {
                    var request = api.getPlaylistsItems(playlistId)
                            .additionalTypes("track")
                            .limit(limit)
                            .offset(offset);
                    if (userMarket != null) {
                        request.market(userMarket);
                    }
                    Paging<PlaylistTrack> paging = request.build().execute();

                    PlaylistTrack[] items = paging.getItems();
                    if (items == null || items.length == 0) break;

                    for (PlaylistTrack item : items) {
                        convertAndAdd(api, item, artistCache, result);
                    }

                    offset += limit;
                    hasMore = paging.getNext() != null;

                } catch (se.michaelthelin.spotify.exceptions.detailed.ForbiddenException fe) {
                    log.warn("Playlist page access restricted (403). Returning {} tracks.", result.size());
                    break;
                } catch (Exception e) {
                    log.warn("Failed to load offset={}: {}", offset, e.getMessage());
                    break;
                }
            }
        }

        log.info("Loaded {} tracks from playlist {}", result.size(), playlistId);
        return result;
    }

    private void convertAndAdd(SpotifyApi api, PlaylistTrack item,
                               Map<String, Artist> artistCache, List<Track> result) {
        if (item == null) {
            return;
        }

        IPlaylistItem playlistItem = item.getTrack();
        if (playlistItem instanceof se.michaelthelin.spotify.model_objects.specification.Track track) {
            if (Boolean.TRUE.equals(item.getIsLocal()) || track.getId() == null || track.getId().isBlank()) {
                log.debug("Skipping local/unidentified track: {}", track.getName());
                return;
            }
            Track converted = convertToTrack(api, track, artistCache);
            result.add(converted);
        } else {
            log.debug("Skipping playlist item type={}",
                    playlistItem != null ? playlistItem.getType() : "null");
        }
    }

    private void convertAndAddJson(SpotifyApi api, JsonNode item,
                                   Map<String, Artist> artistCache, List<Track> result) {
        JsonNode track = item.hasNonNull("track") ? item.path("track") : item.path("item");
        if (track.isMissingNode() || track.isNull() || !"track".equals(track.path("type").asText())) {
            return;
        }

        String spotifyTrackId = text(track, "id");
        String name = text(track, "name");
        if (spotifyTrackId == null || name == null || Boolean.TRUE.equals(item.path("is_local").asBoolean(false))) {
            return;
        }

        String artistName = "Unknown";
        String artistId = null;
        JsonNode artists = track.path("artists");
        if (artists.isArray() && !artists.isEmpty()) {
            JsonNode firstArtist = artists.get(0);
            String firstArtistName = text(firstArtist, "name");
            artistName = firstArtistName != null ? firstArtistName : artistName;
            artistId = text(firstArtist, "id");
        }

        String imageUrl = null;
        JsonNode album = track.path("album");
        if (!album.isMissingNode()) {
            JsonNode images = album.path("images");
            if (images.isArray() && images.size() > 0) {
                imageUrl = text(images.get(0), "url");
            }
        }

        String genres = "";
        boolean isRussian = CyrillicDetector.containsCyrillic(artistName)
                || CyrillicDetector.containsCyrillic(name);

        if (artistId != null) {
            Artist fullArtist = artistCache.computeIfAbsent(artistId, id -> fetchArtist(api, id));
            if (fullArtist != null && fullArtist.getGenres() != null) {
                genres = String.join(",", fullArtist.getGenres());
            }
        }

        result.add(Track.builder()
                .spotifyTrackId(spotifyTrackId)
                .name(name)
                .artistName(artistName)
                .durationMs(track.path("duration_ms").isNumber() ? track.path("duration_ms").asInt() : null)
                .previewUrl(text(track, "preview_url"))
                .imageUrl(imageUrl)
                .genres(genres)
                .isRussian(isRussian)
                .build());
    }

    private Track convertToTrack(SpotifyApi api,
                                 se.michaelthelin.spotify.model_objects.specification.Track track,
                                 Map<String, Artist> artistCache) {
        String artistName = "Unknown";
        String artistId = null;

        if (track.getArtists() != null && track.getArtists().length > 0) {
            var firstArtist = track.getArtists()[0];
            artistName = firstArtist.getName();
            artistId = firstArtist.getId();
        }

        String imageUrl = null;
        if (track.getAlbum() != null && track.getAlbum().getImages() != null && track.getAlbum().getImages().length > 0) {
            imageUrl = track.getAlbum().getImages()[0].getUrl();
        }

        String genres = "";
        boolean isRussian = false;

        if (artistId != null) {
            Artist fullArtist = artistCache.computeIfAbsent(artistId, id -> fetchArtist(api, id));
            if (fullArtist != null) {
                genres = String.join(",", fullArtist.getGenres());
            }
            isRussian = CyrillicDetector.containsCyrillic(artistName)
                     || CyrillicDetector.containsCyrillic(track.getName());
        }

        return Track.builder()
                .spotifyTrackId(track.getId())
                .name(track.getName())
                .artistName(artistName)
                .durationMs(track.getDurationMs())
                .previewUrl(track.getPreviewUrl())
                .imageUrl(imageUrl)
                .genres(genres)
                .isRussian(isRussian)
                .build();
    }

    private Artist fetchArtist(SpotifyApi api, String artistId) {
        try {
            return api.getArtist(artistId).build().execute();
        } catch (Exception e) {
            log.warn("Could not fetch artist {}: {}", artistId, e.getMessage());
            return null;
        }
    }

    private CountryCode resolveUserMarket(SpotifyApi api) {
        try {
            var profile = api.getCurrentUsersProfile().build().execute();
            return profile.getCountry();
        } catch (Exception e) {
            log.debug("Could not resolve Spotify user market: {}", e.getMessage());
            return null;
        }
    }

    private List<CountryCode> marketsToTry(CountryCode userMarket) {
        List<CountryCode> markets = new ArrayList<>();
        markets.add(userMarket);
        if (!CountryCode.PL.equals(userMarket)) {
            markets.add(CountryCode.PL);
        }
        if (!CountryCode.US.equals(userMarket)) {
            markets.add(CountryCode.US);
        }
        if (userMarket != null) {
            markets.add(null);
        }
        return markets;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
