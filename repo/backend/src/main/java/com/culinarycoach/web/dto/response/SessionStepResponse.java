package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionStepResponse(
    Long id,
    int stepOrder,
    String title,
    String description,
    Integer expectedDurationSeconds,
    boolean hasTimer,
    Integer timerDurationSeconds,
    String reminderText,
    boolean completed,
    Instant completedAt,
    List<TipCardResponse> tips
) {}
