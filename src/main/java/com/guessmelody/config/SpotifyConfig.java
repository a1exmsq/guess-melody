package com.guessmelody.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;

import java.net.URI;

/**
 * Configuration for the official Spotify Web API client.
 * Credentials are loaded from the .env file via spring-dotenv.
 *
 * Public playlists are accessed with the Client Credentials flow.
 */
@Configuration
public class SpotifyConfig {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    private static final URI REDIRECT_URI = SpotifyHttpManager
            .makeUri("http://127.0.0.1:8080/api/spotify/callback");

    @Bean
    public SpotifyApi spotifyApi() {
        if (clientId == null || clientId.isBlank() || "your_spotify_client_id_here".equals(clientId)) {
            throw new IllegalStateException(
                    "Spotify Client ID is not configured. Copy .env.example to .env and fill in your credentials."
            );
        }

        return new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(REDIRECT_URI)
                .build();
    }
}
