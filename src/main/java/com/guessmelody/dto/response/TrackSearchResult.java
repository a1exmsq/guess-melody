package com.guessmelody.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrackSearchResult {
    private String spotifyTrackId;
    private String name;
    private String artistName;
    private String albumName;
    private String previewUrl;
    private Integer durationMs;
}
