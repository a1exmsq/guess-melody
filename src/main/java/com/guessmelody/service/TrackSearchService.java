package com.guessmelody.service;

import com.guessmelody.dto.response.TrackSearchResult;

import java.util.List;

/**
 * Track search through an external music provider.
 */
public interface TrackSearchService {

    List<TrackSearchResult> search(String query);
}
