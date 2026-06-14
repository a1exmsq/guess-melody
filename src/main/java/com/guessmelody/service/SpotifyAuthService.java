package com.guessmelody.service;

import com.guessmelody.exception.SpotifyApiException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.net.URI;

/**
 * Manages Spotify OAuth login flow and the authenticated user's API instance.
 *
 * Flow: login URL → callback → access token → user API for playlist import.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyAuthService {

    private final SpotifyApi spotifyApi;

    @Getter
    private volatile SpotifyApi userSpotifyApi;

    public String getAuthorizationUrl() {
        URI uri = spotifyApi.authorizationCodeUri()
                .scope("streaming user-modify-playback-state user-read-playback-state playlist-read-private playlist-read-collaborative user-read-private user-read-email")
                .show_dialog(true)
                .build()
                .execute();
        String url = uri.toString();
        log.info("Generated Spotify OAuth URL");
        return url;
    }

    public void exchangeCode(String code) {
        try {
            AuthorizationCodeCredentials credentials = spotifyApi
                    .authorizationCode(code)
                    .build()
                    .execute();

            userSpotifyApi = new SpotifyApi.Builder()
                    .setClientId(spotifyApi.getClientId())
                    .setClientSecret(spotifyApi.getClientSecret())
                    .setRedirectUri(spotifyApi.getRedirectURI())
                    .setAccessToken(credentials.getAccessToken())
                    .setRefreshToken(credentials.getRefreshToken())
                    .build();

            try {
                var userProfile = userSpotifyApi.getCurrentUsersProfile().build().execute();
                log.info("Spotify token is valid for user: {} ({})",
                        userProfile.getDisplayName(), userProfile.getId());
            } catch (Exception testEx) {
                log.error("Token was returned but is not valid for the API: {}", testEx.getMessage());
                throw new SpotifyApiException("Spotify token is invalid: " + testEx.getMessage(), testEx);
            }

            log.info("Spotify OAuth successful. Access token expires in {}s", credentials.getExpiresIn());
        } catch (Exception e) {
            log.error("Failed to exchange Spotify authorization code", e);
            throw new SpotifyApiException("Spotify authorization failed: " + e.getMessage(), e);
        }
    }

    public boolean isAuthorized() {
        return userSpotifyApi != null && userSpotifyApi.getAccessToken() != null;
    }

    public String getAccessToken() {
        return isAuthorized() ? userSpotifyApi.getAccessToken() : null;
    }

    public SpotifyApi requireUserApi() {
        if (!isAuthorized()) {
            throw new IllegalStateException("Spotify authorization required. Please log in with Spotify.");
        }
        return userSpotifyApi;
    }

    public User getCurrentUserProfile() throws Exception {
        if (!isAuthorized()) {
            throw new IllegalStateException("Not authorized with Spotify");
        }
        return userSpotifyApi.getCurrentUsersProfile().build().execute();
    }
}
