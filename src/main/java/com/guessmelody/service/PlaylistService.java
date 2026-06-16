package com.guessmelody.service;

import com.guessmelody.dto.response.PlaylistAnalyticsResponse;
import com.guessmelody.dto.response.PlaylistImportResponse;
import com.guessmelody.model.entity.Playlist;
import se.michaelthelin.spotify.SpotifyApi;

import java.util.List;

public interface PlaylistService {

    PlaylistImportResponse importFromUrl(String url, SpotifyApi api);

    PlaylistImportResponse importFromTrackUrls(List<String> trackUrls, String name, SpotifyApi api);

    PlaylistAnalyticsResponse getAnalytics(Long playlistId);

    Playlist findById(Long id);
}
