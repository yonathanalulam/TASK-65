package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.Size;

public record UpdatePlaylistRequest(
    @Size(max = 200, message = "Playlist name must not exceed 200 characters")
    String name,
    String description
) {}
