package com.highlands.order.config;

import com.highlands.order.exception.ApiErrorCode;
import com.highlands.order.security.ApiErrorResponseWriter;
import com.highlands.order.security.AuthRateLimitFilter;
import com.highlands.order.security.JwtAuthenticationFilter;
import com.highlands.order.security.SecurityRoutePolicy;
import org.springframework.http.HttpStatus;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, OrderPricingProperties.class})
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            AuthRateLimitFilter rateLimitFilter,
            ApiErrorResponseWriter errorWriter
    ) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/v1/orders/my-orders").authenticated()
                        .requestMatchers("/api/v1/orders/*/approval/**", "/api/v1/orders/*/fulfillment/**").authenticated()
                        .requestMatchers("/api/v1/auth/logout-all", "/api/v1/auth/mfa/**").authenticated()
                        .requestMatchers(SecurityRoutePolicy.PUBLIC_ROUTES).permitAll()
                        .requestMatchers(SecurityRoutePolicy.ADMIN_ROUTES).hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            ApiErrorCode errorCode = (ApiErrorCode) request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_CODE);
                            if (errorCode == null) {
                                errorCode = ApiErrorCode.AUTHENTICATION_FAILED;
                            }
                            String message = errorCode == ApiErrorCode.TOKEN_EXPIRED
                                    ? "Phiên đăng nhập đã hết hạn"
                                    : "Vui lòng đăng nhập để tiếp tục";
                            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED.value(), errorCode, message);
                        })
                        .accessDeniedHandler((request, response, exception) -> errorWriter.write(
                                request,
                                response,
                                HttpStatus.FORBIDDEN.value(),
                                ApiErrorCode.ACCESS_DENIED,
                                "Bạn không có quyền thực hiện thao tác này"
                        )))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
