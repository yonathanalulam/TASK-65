package com.culinarycoach.security.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authenticated principal carrying user identity and session context.
 * This is the ONLY type controllers should use to resolve the authenticated user.
 */
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final Set<String> roles;
    private final String sessionId;
    private final boolean enabled;
    private final boolean accountNonLocked;

    public UserPrincipal(Long userId, String username, String passwordHash,
                          Set<String> roles, String sessionId,
                          boolean enabled, boolean accountNonLocked) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.sessionId = sessionId;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
