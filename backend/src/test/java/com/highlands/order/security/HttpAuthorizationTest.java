package com.highlands.order.security;

import com.highlands.order.config.AuthRateLimitProperties;
import com.highlands.order.config.SecurityConfig;
import com.highlands.order.controller.AdminSecurityAuditController;
import com.highlands.order.controller.AuthController;
import com.highlands.order.exception.GlobalExceptionHandler;
import com.highlands.order.service.*;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HttpAuthorizationTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        reset(TestConfig.AUDIT);
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
        var jwt = context.getBean(JwtService.class);
        var details = context.getBean(DatabaseUserDetailsService.class);
        for (String name : new String[]{"customer", "admin-no-report", "admin-report"}) {
            when(jwt.email(name)).thenReturn(name + "@example.com");
            String[] authorities = name.equals("customer") ? new String[]{"ROLE_CUSTOMER"}
                    : name.equals("admin-report") ? new String[]{"ROLE_ADMIN", "REPORT_READ"}
                    : new String[]{"ROLE_ADMIN"};
            when(details.loadUserByUsername(name + "@example.com")).thenReturn(
                    User.withUsername(name + "@example.com").password("unused").authorities(authorities).build());
        }
        when(TestConfig.AUDIT.search(any(), any(), any(), any())).thenReturn(List.of());
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        if (context != null) context.close();
    }

    @Test
    void auditRequiresBothAdministrativeRouteAccessAndReportPermission() throws Exception {
        String path = "/api/v1/admin/security-audit";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", "Bearer customer"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        mvc.perform(get(path).header("Authorization", "Bearer admin-no-report"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        mvc.perform(get(path).header("Authorization", "Bearer admin-report"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(TestConfig.AUDIT, times(1)).search(null, null, null, null);
    }

    @Test
    void logoutAllAndMfaRejectAnonymousRequestsAndUseAuthenticatedIdentity() throws Exception {
        mvc.perform(post("/api/v1/auth/logout-all"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"));
        mvc.perform(post("/api/v1/auth/mfa/setup")).andExpect(status().isUnauthorized());
        verifyNoInteractions(context.getBean(RefreshSessionService.class), context.getBean(MfaService.class));
        mvc.perform(post("/api/v1/auth/logout-all").header("Authorization", "Bearer customer"))
                .andExpect(status().isOk());
        verify(context.getBean(RefreshSessionService.class)).revokeAll("customer@example.com");
    }

    @Test
    void missingPermissionDoesNotInvokeAuditAndErrorsDoNotLeakInternals() throws Exception {
        var result = mvc.perform(get("/api/v1/admin/security-audit").header("Authorization", "Bearer admin-no-report"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.path").value("/api/v1/admin/security-audit"))
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(result.contains("AccessDeniedException"));
        org.junit.jupiter.api.Assertions.assertFalse(result.contains("PreAuthorize"));
        verifyNoInteractions(TestConfig.AUDIT);
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, AdminSecurityAuditController.class, AuthController.class, GlobalExceptionHandler.class})
    static class TestConfig {
        static final SecurityAuditService AUDIT = mock(SecurityAuditService.class);
        @Bean JwtService jwtService() { return mock(JwtService.class); }
        @Bean DatabaseUserDetailsService details() { return mock(DatabaseUserDetailsService.class); }
        @Bean ApiErrorResponseWriter writer() { return new ApiErrorResponseWriter(new ObjectMapper()); }
        @Bean JwtAuthenticationFilter jwtFilter(JwtService jwt, DatabaseUserDetailsService details, ApiErrorResponseWriter writer) {
            return new JwtAuthenticationFilter(jwt, details, writer);
        }
        @Bean AuthRateLimitFilter rateLimitFilter() { return new AuthRateLimitFilter(new AuthRateLimitProperties(100, 60)); }
        @Bean SecurityAuditService audit() { return AUDIT; }
        @Bean AuthService auth() { return mock(AuthService.class); }
        @Bean RefreshSessionService sessions() { return mock(RefreshSessionService.class); }
        @Bean AccountLifecycleService lifecycle() { return mock(AccountLifecycleService.class); }
        @Bean MfaService mfa() { return mock(MfaService.class); }
    }
}
