package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignStudentRequest(
    @NotNull Long coachUserId,
    @NotNull Long studentUserId
) {}
