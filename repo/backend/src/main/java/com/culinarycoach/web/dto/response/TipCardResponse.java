package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TipCardResponse(
    Long id,
    String title,
    String shortText,
    String detailedText,
    String displayMode,
    boolean enabled
) {}
