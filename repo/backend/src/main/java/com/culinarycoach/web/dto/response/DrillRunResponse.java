package com.culinarycoach.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DrillRunResponse(
    Long id,
    String drillType,
    String status,
    int totalQuestions,
    int correctCount,
    Instant startedAt
) {}
