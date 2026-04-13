package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AudioAssetResponse(
    Long id,
    String title,
    String description,
    String coverArtPath,
    Integer durationSeconds,
    String category,
    String difficulty,
    boolean isFavorite
) {}
