package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record AddPlaylistItemRequest(
    @NotNull(message = "Audio asset ID is required")
    Long audioAssetId
) {}
