package com.guessmelody.repository;

import com.guessmelody.model.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {

    Optional<Track> findBySpotifyTrackId(String spotifyTrackId);

    List<Track> findByPlaylistId(Long playlistId);
}
