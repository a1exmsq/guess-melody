package com.guessmelody.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request to import a Spotify playlist from a URL, URI, or raw id.
 */
@Data
public class PlaylistImportRequest {

    @NotBlank(message = "Playlist URL is required")
    private String url;
}
