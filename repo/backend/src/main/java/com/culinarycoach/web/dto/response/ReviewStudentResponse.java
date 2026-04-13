package com.culinarycoach.web.dto.response;

import com.culinarycoach.domain.entity.User;

public record ReviewStudentResponse(
    Long userId,
    String username,
    String displayName
) {
    public static ReviewStudentResponse from(User user) {
        return new ReviewStudentResponse(
            user.getId(),
            user.getUsername(),
            user.getDisplayName()
        );
    }
}
