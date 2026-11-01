package com.highlands.order.service;

import com.highlands.order.config.AccountSecurityProperties;
import com.highlands.order.dto.LoginRequest;
import com.highlands.order.model.AccountStatus;
import com.highlands.order.model.AppUser;
import com.highlands.order.repository.UserRepository;
import com.highlands.order.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthLockoutTest {
    @Test
    void locksAfterConfiguredFailuresAndKeepsSafeError() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AppUser user = AppUser.builder().email("user@example.com").passwordHash("hash")
                .accountStatus(AccountStatus.ACTIVE).build();
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrong-password", "hash")).thenReturn(false);
        AuthService service = new AuthService(users, encoder, mock(JwtService.class),
                mock(RefreshSessionService.class), new AccountSecurityProperties(2, 15),
                mock(AccountLifecycleService.class));
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");

        BadCredentialsException first = assertThrows(BadCredentialsException.class, () -> service.login(request));
        BadCredentialsException second = assertThrows(BadCredentialsException.class, () -> service.login(request));

        assertEquals(first.getMessage(), second.getMessage());
        assertEquals(AccountStatus.LOCKED, user.getAccountStatus());
        assertNotNull(user.getLockedUntil());
    }
}
