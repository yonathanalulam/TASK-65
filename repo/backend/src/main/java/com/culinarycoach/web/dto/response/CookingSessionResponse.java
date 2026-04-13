package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CookingSessionResponse(
    Long id,
    String recipeTitle,
    Long lessonId,
    String status,
    int totalSteps,
    int lastCompletedStepOrder,
    Instant startedAt,
    Instant completedAt,
    Instant lastActivityAt
) {}
