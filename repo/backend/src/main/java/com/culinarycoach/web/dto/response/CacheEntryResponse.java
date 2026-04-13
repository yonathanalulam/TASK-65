package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CacheEntryResponse(
    Long id,
    Long segmentId,
    String assetTitle,
    String status,
    long fileSizeBytes,
    Instant downloadedAt,
    Instant expiresAt,
    String expiresInLabel
) {}
