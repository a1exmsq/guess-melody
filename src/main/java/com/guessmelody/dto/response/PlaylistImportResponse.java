package com.guessmelody.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of a successful playlist import.
 */
@Data
@Builder
public class PlaylistImportResponse {
    private Long id;
    private String spotifyPlaylistId;
    private String name;
    private int trackCount;
    private List<TrackSummaryResponse> tracks;
    private LocalDateTime createdAt;
}
