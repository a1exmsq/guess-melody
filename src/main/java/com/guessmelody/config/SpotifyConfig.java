package com.guessmelody.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;

import java.net.URI;

@Configuration
public class SpotifyConfig {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri:http://127.0.0.1:8080/api/spotify/callback}")
    private String redirectUri;

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
                .setRedirectUri(SpotifyHttpManager.makeUri(redirectUri))
                .build();
    }
}
