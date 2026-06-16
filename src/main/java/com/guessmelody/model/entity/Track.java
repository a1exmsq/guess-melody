package com.guessmelody.model.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "all_artist_names", length = 1024)
    private String allArtistNames;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "preview_url", length = 512)
    private String previewUrl;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "genres", length = 1024)
    private String genres;

    @Column(name = "is_russian")
    private Boolean isRussian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Playlist playlist;
}
