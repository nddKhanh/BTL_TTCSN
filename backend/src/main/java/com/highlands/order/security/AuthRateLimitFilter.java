package com.highlands.order.security;

import com.highlands.order.config.AuthRateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/login", "/api/v1/auth/resend-verification",
            "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
            "/api/v1/auth/refresh"
    );

    private final AuthRateLimitProperties properties;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(AuthRateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = Instant.now().getEpochSecond();
        String key = clientKey(request) + ':' + request.getRequestURI();
        Window window = windows.compute(key, (ignored, current) ->
                current == null || now >= current.resetAt
                        ? new Window(now + properties.windowSeconds())
                        : current);
        int used = window.count.incrementAndGet();
        int remaining = Math.max(0, properties.requests() - used);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        if (used > properties.requests()) {
            response.setHeader("Retry-After", String.valueOf(Math.max(1, window.resetAt - now)));
            response.sendError(429, "Quá nhiều yêu cầu, vui lòng thử lại sau");
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",", 2)[0].trim();
    }

    private static final class Window {
        private final long resetAt;
        private final AtomicInteger count = new AtomicInteger();
        private Window(long resetAt) { this.resetAt = resetAt; }
    }
}
