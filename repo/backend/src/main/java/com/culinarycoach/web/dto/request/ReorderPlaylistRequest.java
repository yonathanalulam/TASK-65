package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderPlaylistRequest(
    @NotNull(message = "Ordered asset IDs are required")
    List<Long> orderedAssetIds
) {}
