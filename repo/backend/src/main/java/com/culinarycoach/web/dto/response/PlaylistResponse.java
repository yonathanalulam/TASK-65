package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlaylistResponse(
    Long id,
    String name,
    String description,
    int itemCount,
    Instant createdAt
) {}
