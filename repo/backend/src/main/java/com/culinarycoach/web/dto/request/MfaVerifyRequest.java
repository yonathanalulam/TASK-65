package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyRequest(
    @NotBlank(message = "TOTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "TOTP code must be exactly 6 digits")
    String code,

    String mfaToken
) {}
