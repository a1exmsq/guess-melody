package com.guessmelody.service;

import com.guessmelody.exception.SpotifyApiException;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SpotifyAuthService {

    private static final String SESSION_USER_ID_KEY = "SPOTIFY_USER_ID";

    private final SpotifyApi baseApi;
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    private volatile SpotifyApi backendApi;
    private volatile Instant backendTokenExpiresAt = Instant.MIN;

    public SpotifyAuthService(SpotifyApi baseApi) {
        this.baseApi = baseApi;
        this.backendApi = new SpotifyApi.Builder()
                .setClientId(baseApi.getClientId())
                .setClientSecret(baseApi.getClientSecret())
                .setRedirectUri(baseApi.getRedirectURI())
                .build();
    }

    public String getAuthorizationUrl() {
        return baseApi.authorizationCodeUri()
                .scope("streaming user-modify-playback-state user-read-playback-state playlist-read-private playlist-read-collaborative user-read-private user-read-email")
                .show_dialog(true)
                .build()
                .execute()
                .toString();
    }

    public void exchangeCode(String code, HttpSession session) {
        try {
            AuthorizationCodeCredentials credentials = baseApi.authorizationCode(code)
                    .build()
                    .execute();

            SpotifyApi userApi = new SpotifyApi.Builder()
                    .setClientId(baseApi.getClientId())
                    .setClientSecret(baseApi.getClientSecret())
                    .setRedirectUri(baseApi.getRedirectURI())
                    .setAccessToken(credentials.getAccessToken())
                    .setRefreshToken(credentials.getRefreshToken())
                    .build();

            User profile = userApi.getCurrentUsersProfile().build().execute();
            String userId = profile.getId();

            UserSession userSession = new UserSession(
                    userId,
                    profile.getDisplayName(),
                    credentials.getAccessToken(),
                    credentials.getRefreshToken(),
                    Instant.now().plusSeconds(credentials.getExpiresIn() - 60),
                    userApi
            );

            sessions.put(userId, userSession);
            session.setAttribute(SESSION_USER_ID_KEY, userId);

            log.info("Spotify OAuth successful for user: {} ({})", profile.getDisplayName(), userId);
        } catch (Exception e) {
            log.error("Failed to exchange Spotify authorization code", e);
            throw new SpotifyApiException("Spotify authorization failed: " + e.getMessage(), e);
        }
    }

    public boolean isUserAuthorized(HttpSession session) {
        String userId = getUserId(session);
        if (userId == null) {
            return false;
        }
        UserSession userSession = sessions.get(userId);
        return userSession != null && !userSession.isExpired();
    }

    public boolean isBackendAuthorized() {
        refreshBackendTokenIfNeeded();
        return backendApi.getAccessToken() != null;
    }

    public String getAccessToken(HttpSession session) {
        UserSession userSession = requireUserSession(session);
        ensureUserTokenValid(userSession);
        return userSession.accessToken();
    }

    public SpotifyApi requireUserApi(HttpSession session) {
        UserSession userSession = requireUserSession(session);
        ensureUserTokenValid(userSession);
        return userSession.api();
    }

    public SpotifyApi requireBackendApi() {
        refreshBackendTokenIfNeeded();
        if (backendApi.getAccessToken() == null) {
            throw new SpotifyApiException("Backend Spotify token is not available");
        }
        return backendApi;
    }

    public User getCurrentUserProfile(HttpSession session) {
        try {
            return requireUserApi(session).getCurrentUsersProfile().build().execute();
        } catch (Exception e) {
            throw new SpotifyApiException("Failed to fetch Spotify profile: " + e.getMessage(), e);
        }
    }

    public void logout(HttpSession session) {
        String userId = getUserId(session);
        if (userId != null) {
            sessions.remove(userId);
        }
        session.removeAttribute(SESSION_USER_ID_KEY);
    }

    private UserSession requireUserSession(HttpSession session) {
        String userId = getUserId(session);
        if (userId == null) {
            throw new SpotifyApiException(401, "Spotify authorization required. Please log in.", null);
        }
        UserSession userSession = sessions.get(userId);
        if (userSession == null) {
            throw new SpotifyApiException(401, "Spotify session not found. Please log in again.", null);
        }
        return userSession;
    }

    private String getUserId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object userId = session.getAttribute(SESSION_USER_ID_KEY);
        return userId instanceof String ? (String) userId : null;
    }

    private void ensureUserTokenValid(UserSession userSession) {
        if (userSession.isExpired()) {
            refreshUserToken(userSession);
        }
    }

    private void refreshUserToken(UserSession userSession) {
        try {
            SpotifyApi refreshApi = new SpotifyApi.Builder()
                    .setClientId(baseApi.getClientId())
                    .setClientSecret(baseApi.getClientSecret())
                    .setRefreshToken(userSession.refreshToken())
                    .build();

            AuthorizationCodeCredentials credentials = refreshApi.authorizationCodeRefresh()
                    .build()
                    .execute();

            SpotifyApi newApi = new SpotifyApi.Builder()
                    .setClientId(baseApi.getClientId())
                    .setClientSecret(baseApi.getClientSecret())
                    .setRedirectUri(baseApi.getRedirectURI())
                    .setAccessToken(credentials.getAccessToken())
                    .setRefreshToken(userSession.refreshToken())
                    .build();

            UserSession refreshed = new UserSession(
                    userSession.userId(),
                    userSession.displayName(),
                    credentials.getAccessToken(),
                    userSession.refreshToken(),
                    Instant.now().plusSeconds(credentials.getExpiresIn() - 60),
                    newApi
            );

            sessions.put(userSession.userId(), refreshed);
            log.info("Refreshed Spotify access token for user {}", userSession.userId());
        } catch (Exception e) {
            log.error("Failed to refresh Spotify token for user {}", userSession.userId(), e);
            sessions.remove(userSession.userId());
            throw new SpotifyApiException(401, "Spotify session expired. Please log in again.", null);
        }
    }

    private synchronized void refreshBackendTokenIfNeeded() {
        if (Instant.now().isBefore(backendTokenExpiresAt) && backendApi.getAccessToken() != null) {
            return;
        }
        try {
            ClientCredentials credentials = baseApi.clientCredentials()
                    .build()
                    .execute();
            backendApi.setAccessToken(credentials.getAccessToken());
            backendTokenExpiresAt = Instant.now().plusSeconds(credentials.getExpiresIn() - 60);
            log.info("Obtained Spotify backend token, expires in {}s", credentials.getExpiresIn());
        } catch (Exception e) {
            log.error("Failed to obtain Spotify backend token", e);
            throw new SpotifyApiException("Spotify backend authorization failed: " + e.getMessage(), e);
        }
    }

    private record UserSession(
            String userId,
            String displayName,
            String accessToken,
            String refreshToken,
            Instant expiresAt,
            SpotifyApi api
    ) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
