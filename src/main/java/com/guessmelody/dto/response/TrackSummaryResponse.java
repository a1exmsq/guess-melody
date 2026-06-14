package com.guessmelody.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Short track summary returned in playlist import previews.
 */
@Data
@Builder
public class TrackSummaryResponse {
    private String spotifyTrackId;
    private String name;
    private String artistName;
    private Integer durationMs;
}
