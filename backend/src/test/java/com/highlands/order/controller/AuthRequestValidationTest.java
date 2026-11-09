package com.highlands.order.controller;

import com.highlands.order.exception.GlobalExceptionHandler;
import com.highlands.order.service.AccountLifecycleService;
import com.highlands.order.service.AuthService;
import com.highlands.order.service.MfaService;
import com.highlands.order.service.RefreshSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthRequestValidationTest {
    private final AuthService auth = mock(AuthService.class);
    private final AccountLifecycleService lifecycle = mock(AccountLifecycleService.class);
    private final RefreshSessionService sessions = mock(RefreshSessionService.class);
    private final MfaService mfa = mock(MfaService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(auth, sessions, lifecycle, mfa))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingFieldsReturnValidationErrorsBeforeCallingServices() throws Exception {
        for (String endpoint : new String[]{"refresh", "logout", "verify-email", "resend-verification",
                "forgot-password", "reset-password", "mfa/confirm", "mfa/challenge"}) {
            mvc.perform(post("/api/v1/auth/" + endpoint).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(auth, lifecycle, sessions, mfa);
    }

    @Test
    void rejectsMalformedEmailTokenPasswordAndMfaValues() throws Exception {
        String[][] cases = {
                {"forgot-password", "{\"email\":\"not-an-email\"}"},
                {"resend-verification", "{\"email\":\"  \"}"},
                {"refresh", "{\"refreshToken\":\"" + "x".repeat(129) + "\"}"},
                {"verify-email", "{\"token\":null}"},
                {"reset-password", "{\"token\":\"sample\",\"newPassword\":\"short\"}"},
                {"reset-password", "{\"token\":\"sample\",\"newPassword\":\"" + "x".repeat(73) + "\"}"},
                {"mfa/confirm", "{\"code\":\"12345\"}"},
                {"mfa/challenge", "{\"code\":\"not-a-recovery-code\"}"}
        };
        for (String[] request : cases) {
            mvc.perform(post("/api/v1/auth/" + request[0]).contentType(MediaType.APPLICATION_JSON).content(request[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(auth, lifecycle, sessions, mfa);
    }

    @Test
    void acceptsValidLifecycleRequestsWithoutChangingTheirValues() throws Exception {
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"one-time-token\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(lifecycle).verifyEmail("one-time-token");
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"reset-token\",\"newPassword\":\"sample-password\"}"))
                .andExpect(status().isOk());
        verify(lifecycle).resetPassword("reset-token", "sample-password");
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token\"}"))
                .andExpect(status().isOk());
        verify(sessions).rotate("refresh-token");
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"person@example.com\"}"))
                .andExpect(status().isOk());
        verify(lifecycle).requestPasswordReset("person@example.com");
    }

    @Test
    void acceptsTotpAndRecoveryCodeShapesAndRejectsMalformedJson() throws Exception {
        var user = User.withUsername("person@example.com").password("unused").roles("CUSTOMER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        for (String code : new String[]{"123456", "ABCDEF0123456789"}) {
            mvc.perform(post("/api/v1/auth/mfa/challenge").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"" + code + "\"}"))
                    .andExpect(status().isOk());
            verify(mfa).challenge("person@example.com", code);
        }
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_JSON"));
        verifyNoInteractions(sessions);
    }
}
