package com.guessmelody.controller;

import com.guessmelody.service.SpotifyAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyAuthController {

    private final SpotifyAuthService authService;

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        String url = authService.getAuthorizationUrl();
        log.info("Generated Spotify OAuth URL");
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description) {

        if (error != null) {
            log.error("Spotify returned error: {} — {}", error, error_description);
            return ResponseEntity.badRequest().body(
                    "Spotify error: " + (error_description != null ? error_description : error) +
                    "\n\nAuthorization was cancelled or failed. Please try again."
            );
        }

        if (code == null || code.isBlank()) {
            log.error("Callback called without code and without error");
            return ResponseEntity.badRequest().body(
                    "Authorization code was not received from Spotify.\n" +
                    "Make sure the redirect URI in the Spotify Dashboard is set to http://127.0.0.1:8080/api/spotify/callback"
            );
        }

        log.info("Received Spotify authorization code, exchanging for token...");
        authService.exchangeCode(code);
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body("""
                        <!DOCTYPE html>
                        <html>
                        <head><meta charset="UTF-8"><title>Authorized</title></head>
                        <body style="font-family:sans-serif;text-align:center;padding:50px;background:#121212;color:#fff;">
                            <h1 style="color:#6366f1;">Authorization successful!</h1>
                            <p>You will be redirected back to the game in 3 seconds...</p>
                            <p><a href="/" style="color:#6366f1;">← Return now</a></p>
                            <script>setTimeout(()=>location.href='/',3000);</script>
                        </body>
                        </html>
                        """);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("authorized", authService.isAuthorized()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        try {
            if (!authService.isAuthorized()) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authorized"));
            }
            var profile = authService.getCurrentUserProfile();
            return ResponseEntity.ok(Map.of(
                    "displayName", profile.getDisplayName(),
                    "id", profile.getId(),
                    "email", profile.getEmail() != null ? profile.getEmail() : "hidden",
                    "country", profile.getCountry() != null ? profile.getCountry().name() : "unknown",
                    "images", profile.getImages() != null && profile.getImages().length > 0
                            ? profile.getImages()[0].getUrl()
                            : null
            ));
        } catch (Exception e) {
            log.error("Failed to fetch Spotify profile", e);
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/token")
    public ResponseEntity<?> getToken() {
        try {
            String token = authService.getAccessToken();
            if (token == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authorized"));
            }
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "expiresIn", 3600
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
