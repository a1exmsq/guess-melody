package com.guessmelody.service;

import com.guessmelody.dto.response.TrackSearchResult;
import com.guessmelody.model.entity.Track;

import java.util.List;

/**
 * In-memory game track pool used by single-player and fallback multiplayer modes.
 */
public interface GameTrackPoolService {

    Track addTrack(TrackSearchResult searchResult);

    List<Track> getAllTracks();

    Track getRandomTrack();

    void clearPool();

    long getTrackCount();
}
