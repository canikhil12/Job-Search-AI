package com.jobmatch.auth.jwt;

import com.jobmatch.user.User;
import com.jobmatch.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads a Bearer token, validates it, and populates the SecurityContext for the
 * duration of the request. Any failure simply leaves the context unauthenticated —
 * downstream authorization rules then reject protected endpoints with 401/403.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token, request);
        }
        filterChain.doFilter(request, response);
    }

    // Also run on ASYNC dispatches (e.g. SSE stream completion). Otherwise the SecurityContext is
    // empty on the async re-dispatch while Spring Security's AuthorizationFilter still runs there,
    // which would reject the request with AccessDenied and abort the stream.
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            UUID userId = UUID.fromString(jwtService.extractSubject(token));
            Optional<User> user = userRepository.findById(userId);
            user.ifPresent(u -> {
                var authentication = new UsernamePasswordAuthenticationToken(u, null, List.of());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        } catch (Exception ex) {
            // Invalid / expired / tampered token, or malformed subject -> stay unauthenticated.
            SecurityContextHolder.clearContext();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
