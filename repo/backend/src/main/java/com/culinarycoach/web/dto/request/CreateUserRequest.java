package com.culinarycoach.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 64, message = "Username must be 3-64 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain alphanumeric, '.', '_', '-'")
    String username,

    @NotBlank(message = "Password is required")
    String password,

    String displayName,
    String email,
    Set<String> roles
) {}
