package com.guessmelody.repository;

import com.guessmelody.model.entity.Playlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    Optional<Playlist> findBySpotifyPlaylistId(String spotifyPlaylistId);

    @EntityGraph(attributePaths = "tracks")
    Optional<Playlist> findWithTracksById(Long id);
}
