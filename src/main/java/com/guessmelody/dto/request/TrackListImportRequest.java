package com.guessmelody.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TrackListImportRequest {

    @NotBlank(message = "Playlist name is required")
    private String name;

    @NotEmpty(message = "At least one track link is required")
    private List<@NotBlank(message = "Track link cannot be empty") String> trackUrls;
}
