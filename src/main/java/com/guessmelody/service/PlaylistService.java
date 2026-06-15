package com.guessmelody.service;

import com.guessmelody.dto.response.PlaylistAnalyticsResponse;
import com.guessmelody.dto.response.PlaylistImportResponse;
import com.guessmelody.model.entity.Playlist;

public interface PlaylistService {

    PlaylistImportResponse importFromUrl(String url);

    PlaylistAnalyticsResponse getAnalytics(Long playlistId);

    Playlist findById(Long id);
}
