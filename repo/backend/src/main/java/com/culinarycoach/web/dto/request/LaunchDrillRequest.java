package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record LaunchDrillRequest(
    @NotNull(message = "Entry ID is required")
    Long entryId
) {}
