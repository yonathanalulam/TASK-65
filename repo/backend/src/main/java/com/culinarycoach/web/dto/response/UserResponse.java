package com.culinarycoach.web.dto.response;

import com.culinarycoach.domain.entity.User;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
    Long id,
    String username,
    String displayName,
    String email,
    String status,
    boolean mfaEnabled,
    Set<String> roles,
    Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getEmail(),
            user.getStatus().name(),
            user.isMfaEnabled(),
            user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()),
            user.getCreatedAt()
        );
    }
}
