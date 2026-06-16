package com.guessmelody.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${site.auth.user:admin}")
    private String siteAuthUser;

    @Value("${site.auth.password:}")
    private String siteAuthPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean passwordEnabled = siteAuthPassword != null && !siteAuthPassword.isBlank();

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/api/spotify/callback", "/api/spotify/login", "/api/spotify/status").permitAll();
                auth.requestMatchers("/ws/**", "/topic/**", "/app/**").permitAll();
                auth.requestMatchers("/h2-console/**").permitAll();
                if (passwordEnabled) {
                    auth.anyRequest().authenticated();
                } else {
                    auth.anyRequest().permitAll();
                }
            })
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        if (passwordEnabled) {
            http.httpBasic(Customizer.withDefaults());
        }

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        String password = (siteAuthPassword != null && !siteAuthPassword.isBlank())
                ? siteAuthPassword
                : java.util.UUID.randomUUID().toString();

        UserDetails user = User.builder()
                .username(siteAuthUser)
                .password(passwordEncoder.encode(password))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(parseOrigins(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> parseOrigins(String origins) {
        if (origins == null || origins.isBlank()) {
            return List.of("*");
        }
        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
