package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTimerRequest(
    Long stepId,

    @Size(max = 200, message = "Label must be at most 200 characters")
    String label,

    @NotNull(message = "Duration in seconds is required")
    @Min(value = 1, message = "Duration must be at least 1 second")
    @Max(value = 86400, message = "Duration must be at most 86400 seconds (24 hours)")
    Integer durationSeconds
) {}
