package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GenerateExportRequest(
    @NotNull(message = "Business date is required")
    LocalDate businessDate
) {}
