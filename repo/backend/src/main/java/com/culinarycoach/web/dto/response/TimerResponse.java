package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TimerResponse(
    Long id,
    Long stepId,
    String label,
    String timerType,
    String status,
    int durationSeconds,
    Long remainingSeconds,
    Instant startedAt,
    Instant targetEndAt,
    Instant pausedAt,
    Instant acknowledgedAt
) {}
