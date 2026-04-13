package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VoidTransactionRequest(
    @NotBlank(message = "Void reason is required")
    String reason
) {}
