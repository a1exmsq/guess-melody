package com.guessmelody.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Analytics for an imported playlist.
 */
@Data
@Builder
public class PlaylistAnalyticsResponse {
    private int totalTracks;
    private int russianTracks;
    private int foreignTracks;
    private double russianPercent;
    private double foreignPercent;
    private Map<String, Long> topGenres;
}
