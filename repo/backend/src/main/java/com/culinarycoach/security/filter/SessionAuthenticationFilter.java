package com.culinarycoach.security.filter;

import com.culinarycoach.config.AppProperties;
import com.culinarycoach.domain.entity.AuthSession;
import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.enums.AccountStatus;
import com.culinarycoach.domain.enums.SessionStatus;
import com.culinarycoach.domain.repository.AuthSessionRepository;
import com.culinarycoach.domain.repository.UserRepository;
import com.culinarycoach.security.auth.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Session authentication filter that reads X-Session-Id header (or session cookie),
 * validates the backing auth session, resolves the user, and populates SecurityContext.
 *
 * This is the ONLY mechanism by which authenticated identity enters the security context
 * for API requests.
 */
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String SESSION_HEADER = "X-Session-Id";

    private final AuthSessionRepository authSessionRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    public SessionAuthenticationFilter(AuthSessionRepository authSessionRepository,
                                        UserRepository userRepository,
                                        AppProperties appProperties) {
        this.authSessionRepository = authSessionRepository;
        this.userRepository = userRepository;
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // Only attempt auth if not already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && !(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof String)) {
            filterChain.doFilter(request, response);
            return;
        }

        String sessionId = request.getHeader(SESSION_HEADER);
        if (sessionId == null || sessionId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthSession session = authSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate session status
        if (!session.isActive()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check absolute expiration
        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setStatus(SessionStatus.ABSOLUTE_EXPIRED);
            authSessionRepository.save(session);
            filterChain.doFilter(request, response);
            return;
        }

        // Check idle timeout
        int idleMinutes = appProperties.getSecurity().getIdleTimeoutMinutes();
        if (session.getLastAccessedAt().plus(idleMinutes, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            session.setStatus(SessionStatus.IDLE_EXPIRED);
            authSessionRepository.save(session);
            filterChain.doFilter(request, response);
            return;
        }

        // Load user
        User user = userRepository.findById(session.getUserId()).orElse(null);
        if (user == null || user.getStatus() != AccountStatus.ACTIVE) {
            filterChain.doFilter(request, response);
            return;
        }

        // Touch session last-accessed
        session.setLastAccessedAt(Instant.now());
        session.setStatus(SessionStatus.ACTIVE);
        authSessionRepository.save(session);

        // Build principal
        Set<String> roles = user.getRoles().stream()
            .map(r -> r.getName())
            .collect(Collectors.toSet());

        UserPrincipal principal = new UserPrincipal(
            user.getId(),
            user.getUsername(),
            user.getPasswordHash(),
            roles,
            sessionId,
            true,
            !user.isAccountLocked()
        );

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
