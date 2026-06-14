package com.guessmelody.model.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Spotify track metadata stored for the game.
 *
 * Note: title and artist are not exposed to the frontend directly — they are
 * revealed only after the round ends to prevent cheating.
 */
@Entity
@Table(name = "tracks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spotify_track_id", nullable = false, unique = true, length = 64)
    private String spotifyTrackId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "artist_name", nullable = false)
    private String artistName;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "preview_url", length = 512)
    private String previewUrl;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    /** Comma-separated artist genres used for playlist analytics, e.g. "rock,pop,indie". */
    @Column(name = "genres", length = 1024)
    private String genres;

    /** True when the artist or title contains Cyrillic characters. */
    @Column(name = "is_russian")
    private Boolean isRussian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Playlist playlist;
}
