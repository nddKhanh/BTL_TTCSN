package com.highlands.order.security;

import com.highlands.order.config.AuthRateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthRateLimitFilterTest {
    @Test
    void limitsByClientAndExposesRetryHeaders() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new AuthRateLimitProperties(2, 60));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilterInternal(request, response, chain);
        filter.doFilterInternal(request, response, chain);
        filter.doFilterInternal(request, response, chain);

        verify(chain, times(2)).doFilter(request, response);
        verify(response, times(2)).setHeader("X-RateLimit-Remaining", "0");
        verify(response).sendError(429, "Quá nhiều yêu cầu, vui lòng thử lại sau");
    }
}
