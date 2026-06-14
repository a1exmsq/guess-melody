package com.guessmelody.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guessmelody.dto.response.TrackSearchResult;
import com.guessmelody.exception.SpotifyApiException;
import com.guessmelody.service.SpotifyAuthService;
import com.guessmelody.service.TrackSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Spotify Web API track search implementation.
 */
@Slf4j
@Service
public class SpotifyTrackSearchService implements TrackSearchService {

    private final SpotifyApi spotifyApi;
    private final SpotifyAuthService spotifyAuthService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private volatile long tokenExpiresAt = 0;

    public SpotifyTrackSearchService(SpotifyApi spotifyApi, SpotifyAuthService spotifyAuthService) {
        this.spotifyApi = spotifyApi;
        this.spotifyAuthService = spotifyAuthService;
        this.restTemplate = new RestTemplate(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
        this.restTemplate.setInterceptors(Collections.singletonList((request, body, execution) -> {
            log.debug("Spotify API Request: {} {}", request.getMethod(), request.getURI());
            return execution.execute(request, body);
        }));
    }

    @Override
    public synchronized List<TrackSearchResult> search(String query) {
        String token = resolveToken();
        int limit = 10;

        try {
            java.net.URI uri = UriComponentsBuilder
                    .fromUriString("https://api.spotify.com/v1/search")
                    .queryParam("q", query)
                    .queryParam("type", "track")
                    .queryParam("limit", limit)
                    .queryParam("market", "PL")
                    .encode()
                    .build()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("Spotify search URL: {}", uri);
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("tracks").path("items");

            List<TrackSearchResult> results = new ArrayList<>();
            for (JsonNode item : items) {
                results.add(convertJson(item));
            }

            log.info("Search '{}' returned {} tracks", query, results.size());
            return results;

        } catch (Exception e) {
            log.error("Track search failed for query '{}': {}", query, e.getMessage());
            throw new SpotifyApiException("Track search failed: " + e.getMessage(), e);
        }
    }

    private String resolveToken() {
        if (spotifyAuthService.isAuthorized()) {
            return spotifyAuthService.getAccessToken();
        }
        ensureAccessToken();
        return spotifyApi.getAccessToken();
    }

    private TrackSearchResult convertJson(JsonNode track) {
        String artistName = "Unknown";
        JsonNode artists = track.path("artists");
        if (artists.isArray() && artists.size() > 0) {
            artistName = artists.get(0).path("name").asText("Unknown");
        }

        String albumName = track.path("album").path("name").asText("Unknown");

        return TrackSearchResult.builder()
                .spotifyTrackId(track.path("id").asText())
                .name(track.path("name").asText())
                .artistName(artistName)
                .albumName(albumName)
                .previewUrl(track.path("preview_url").asText(null))
                .durationMs((int) track.path("duration_ms").asLong())
                .build();
    }

    private synchronized void ensureAccessToken() {
        try {
            long now = System.currentTimeMillis();
            if (spotifyApi.getAccessToken() == null || now >= tokenExpiresAt) {
                ClientCredentialsRequest request = spotifyApi.clientCredentials().build();
                ClientCredentials credentials = request.execute();
                spotifyApi.setAccessToken(credentials.getAccessToken());
                // Refresh a minute before the token actually expires.
                tokenExpiresAt = now + (credentials.getExpiresIn() - 60) * 1000L;
                log.info("Obtained Spotify access token for search, expires in {}s", credentials.getExpiresIn());
            }
        } catch (Exception e) {
            throw new SpotifyApiException(
                    "Could not authenticate with Spotify API. Check SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET in .env", e);
        }
    }
}
