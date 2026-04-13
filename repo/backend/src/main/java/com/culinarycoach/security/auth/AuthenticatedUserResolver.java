package com.culinarycoach.security.auth;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Centralized utility for resolving the authenticated user from SecurityContext.
 * All controllers and services should use this instead of manual casting.
 */
@Component
public class AuthenticatedUserResolver {

    /**
     * Get the current authenticated UserPrincipal.
     * @throws AccessDeniedException if no valid authentication exists
     */
    public UserPrincipal require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("Authentication required");
        }
        return (UserPrincipal) auth.getPrincipal();
    }

    /**
     * Get the current authenticated UserPrincipal from an explicit Authentication.
     * @throws AccessDeniedException if authentication is invalid
     */
    public UserPrincipal require(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("Authentication required");
        }
        return (UserPrincipal) authentication.getPrincipal();
    }

    /**
     * Get the user ID of the currently authenticated user.
     * @throws AccessDeniedException if not authenticated
     */
    public Long requireUserId() {
        return require().getUserId();
    }

    /**
     * Get the user ID from an explicit Authentication.
     * @throws AccessDeniedException if not authenticated
     */
    public Long requireUserId(Authentication authentication) {
        return require(authentication).getUserId();
    }

    /**
     * Get the session ID of the current authentication.
     */
    public String requireSessionId() {
        return require().getSessionId();
    }
}
