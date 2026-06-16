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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class SpotifyTrackSearchService implements TrackSearchService {

    private final SpotifyAuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    public SpotifyTrackSearchService(SpotifyAuthService authService) {
        this.authService = authService;
        this.restTemplate = new RestTemplate(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
        this.restTemplate.setInterceptors(Collections.singletonList((request, body, execution) -> {
            log.debug("Spotify API Request: {} {}", request.getMethod(), request.getURI());
            return execution.execute(request, body);
        }));
    }

    @Override
    public List<TrackSearchResult> search(String query) {
        String token = authService.requireBackendApi().getAccessToken();
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

    private TrackSearchResult convertJson(JsonNode track) {
        String artistName = "Unknown";
        String allArtistNames = null;
        JsonNode artists = track.path("artists");
        if (artists.isArray() && artists.size() > 0) {
            List<String> names = new ArrayList<>();
            for (JsonNode artist : artists) {
                names.add(artist.path("name").asText("Unknown"));
            }
            artistName = names.get(0);
            allArtistNames = String.join(",", names);
        }

        String albumName = track.path("album").path("name").asText("Unknown");

        return TrackSearchResult.builder()
                .spotifyTrackId(track.path("id").asText())
                .name(track.path("name").asText())
                .artistName(artistName)
                .allArtistNames(allArtistNames)
                .albumName(albumName)
                .previewUrl(track.path("preview_url").asText(null))
                .durationMs((int) track.path("duration_ms").asLong())
                .build();
    }
}
