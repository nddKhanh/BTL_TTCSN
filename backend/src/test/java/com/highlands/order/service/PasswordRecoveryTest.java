package com.highlands.order.service;

import com.highlands.order.mail.FakeMailGateway;
import com.highlands.order.model.AppUser;
import com.highlands.order.model.AuthToken;
import com.highlands.order.model.RefreshSession;
import com.highlands.order.repository.AuthTokenRepository;
import com.highlands.order.repository.RefreshSessionRepository;
import com.highlands.order.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordRecoveryTest {
    @Test
    void resetIsOneTimeAndRevokesActiveSessions() {
        AuthTokenRepository tokens = mock(AuthTokenRepository.class);
        RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AppUser user = AppUser.builder().id(9L).passwordHash("old").build();
        AuthToken token = AuthToken.builder().user(user).purpose(AuthToken.TokenPurpose.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
        RefreshSession session = RefreshSession.builder().user(user)
                .expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(tokens.findByTokenHashAndPurpose(anyString(), eq(AuthToken.TokenPurpose.PASSWORD_RESET)))
                .thenReturn(Optional.of(token));
        when(sessions.findAllByUserIdAndRevokedAtIsNull(9L)).thenReturn(List.of(session));
        when(encoder.encode("new-password")).thenReturn("new-hash");
        AccountLifecycleService service = new AccountLifecycleService(tokens, mock(UserRepository.class),
                new FakeMailGateway(), encoder, sessions);

        service.resetPassword("raw-token", "new-password");

        assertEquals("new-hash", user.getPasswordHash());
        assertNotNull(user.getPasswordChangedAt());
        assertNotNull(token.getUsedAt());
        assertNotNull(session.getRevokedAt());
        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("raw-token", "new-password"));
    }
}
