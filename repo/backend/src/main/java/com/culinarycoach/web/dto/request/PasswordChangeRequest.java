package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
    @NotBlank(message = "Current password is required")
    String currentPassword,
    @NotBlank(message = "New password is required")
    String newPassword
) {}
