package com.highlands.order.security;

import com.highlands.order.exception.ApiErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String JWT_ERROR_CODE = JwtAuthenticationFilter.class.getName() + ".errorCode";

    private final JwtService jwtService;
    private final DatabaseUserDetailsService userDetailsService;
    private final ApiErrorResponseWriter errorWriter;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            DatabaseUserDetailsService userDetailsService,
            ApiErrorResponseWriter errorWriter
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ") || SecurityContextHolder.getContext().getAuthentication() != null) { filterChain.doFilter(request, response); return; }
        try {
            UserDetails user = userDetailsService.loadUserByUsername(jwtService.subject(header.substring(7)));
            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException ex) {
            writeAuthenticationError(request, response, ApiErrorCode.TOKEN_EXPIRED, "Phiên đăng nhập đã hết hạn");
            return;
        } catch (JwtException | UsernameNotFoundException ex) {
            writeAuthenticationError(request, response, ApiErrorCode.AUTHENTICATION_FAILED, "Phiên đăng nhập không hợp lệ");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeAuthenticationError(
            HttpServletRequest request,
            HttpServletResponse response,
            ApiErrorCode errorCode,
            String message
    ) throws IOException {
        request.setAttribute(JWT_ERROR_CODE, errorCode);
        errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, errorCode, message);
    }
}
