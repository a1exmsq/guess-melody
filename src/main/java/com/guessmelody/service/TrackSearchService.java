package com.guessmelody.service;

import com.guessmelody.dto.response.TrackSearchResult;

import java.util.List;

public interface TrackSearchService {

    List<TrackSearchResult> search(String query);
}
