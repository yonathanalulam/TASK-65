package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePlaylistRequest(
    @NotBlank(message = "Playlist name is required")
    @Size(max = 200, message = "Playlist name must not exceed 200 characters")
    String name,
    String description
) {}
