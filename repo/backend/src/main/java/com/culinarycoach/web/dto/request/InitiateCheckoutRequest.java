package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InitiateCheckoutRequest(
    @NotEmpty(message = "At least one bundle ID is required")
    List<Long> bundleIds
) {}
