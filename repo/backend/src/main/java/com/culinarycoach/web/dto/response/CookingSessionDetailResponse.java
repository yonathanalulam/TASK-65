package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CookingSessionDetailResponse(
    Long id,
    String recipeTitle,
    Long lessonId,
    String status,
    int totalSteps,
    int lastCompletedStepOrder,
    Instant startedAt,
    Instant resumedAt,
    Instant completedAt,
    Instant abandonedAt,
    Instant lastActivityAt,
    List<SessionStepResponse> steps,
    List<TimerResponse> timers
) {}
