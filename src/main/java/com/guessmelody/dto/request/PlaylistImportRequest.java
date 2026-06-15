package com.guessmelody.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlaylistImportRequest {

    @NotBlank(message = "Playlist URL is required")
    private String url;
}
