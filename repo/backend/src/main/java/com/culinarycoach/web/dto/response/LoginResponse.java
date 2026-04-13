package com.culinarycoach.web.dto.response;

import java.util.Set;

public record LoginResponse(
    Long userId,
    String username,
    String displayName,
    Set<String> roles,
    boolean mfaRequired,
    String mfaToken,
    boolean forcePasswordChange,
    String signingKey,
    String sessionId
) {
    public static LoginResponse mfaRequired(String mfaToken) {
        return new LoginResponse(null, null, null, null, true, mfaToken, false, null, null);
    }

    public static LoginResponse success(Long userId, String username, String displayName,
                                         Set<String> roles, boolean forcePasswordChange,
                                         String signingKey, String sessionId) {
        return new LoginResponse(userId, username, displayName, roles, false, null,
            forcePasswordChange, signingKey, sessionId);
    }
}
