package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfigureTipRequest(
    @NotBlank(message = "Scope is required")
    String scope,

    Long scopeId,

    @NotNull(message = "Display mode is required")
    String displayMode
) {}
