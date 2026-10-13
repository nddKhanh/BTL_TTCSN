package com.highlands.order.security;

import com.highlands.order.exception.ApiErrorCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private DatabaseUserDetailsService userDetailsService;
    @Mock
    private ApiErrorResponseWriter errorWriter;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @Test
    void invalidBearerToken_returnsUnauthorizedInsteadOfContinuingAsGuest() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService, errorWriter);
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtService.subject("invalid-token")).thenThrow(new JwtException("invalid"));

        filter.doFilterInternal(request, response, filterChain);

        verify(errorWriter).write(
                request,
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                ApiErrorCode.AUTHENTICATION_FAILED,
                "Phiên đăng nhập không hợp lệ"
        );
        verify(filterChain, never()).doFilter(request, response);
    }
}
