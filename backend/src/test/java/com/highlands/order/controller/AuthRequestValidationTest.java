package com.highlands.order.controller;

import com.highlands.order.exception.GlobalExceptionHandler;
import com.highlands.order.service.AccountLifecycleService;
import com.highlands.order.service.AuthService;
import com.highlands.order.service.MfaService;
import com.highlands.order.service.RefreshSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
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
}
