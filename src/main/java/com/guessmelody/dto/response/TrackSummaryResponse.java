package com.guessmelody.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrackSummaryResponse {
    private String spotifyTrackId;
    private String name;
    private String artistName;
    private Integer durationMs;
}
