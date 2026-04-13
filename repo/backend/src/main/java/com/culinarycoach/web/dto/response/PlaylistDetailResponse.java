package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlaylistDetailResponse(
    Long id,
    String name,
    String description,
    List<AudioAssetResponse> items
) {}
