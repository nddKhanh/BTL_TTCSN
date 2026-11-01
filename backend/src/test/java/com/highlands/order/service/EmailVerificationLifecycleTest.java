package com.highlands.order.service;

import com.highlands.order.mail.FakeMailGateway;
import com.highlands.order.model.AccountStatus;
import com.highlands.order.model.AppUser;
import com.highlands.order.model.AuthToken;
import com.highlands.order.repository.AuthTokenRepository;
import com.highlands.order.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EmailVerificationLifecycleTest {
    @Test
    void storesOnlyHashAndActivatesTokenOnce() {
        AuthTokenRepository tokens = mock(AuthTokenRepository.class);
        UserRepository users = mock(UserRepository.class);
        FakeMailGateway mail = new FakeMailGateway();
        AccountLifecycleService service = new AccountLifecycleService(tokens, users, mail);
        AppUser user = AppUser.builder().id(7L).email("user@example.com")
                .accountStatus(AccountStatus.PENDING_VERIFICATION).build();
        ArgumentCaptor<AuthToken> captor = ArgumentCaptor.forClass(AuthToken.class);

        service.issueVerification(user);
        verify(tokens).save(captor.capture());
        assertEquals(64, captor.getValue().getTokenHash().length());
        assertFalse(mail.outbox().get(0).body().contains(captor.getValue().getTokenHash()));

        AuthToken stored = AuthToken.builder().user(user)
                .purpose(AuthToken.TokenPurpose.EMAIL_VERIFICATION)
                .tokenHash(captor.getValue().getTokenHash()).expiresAt(LocalDateTime.now().plusMinutes(5)).build();
        when(tokens.findByTokenHashAndPurpose(anyString(), eq(AuthToken.TokenPurpose.EMAIL_VERIFICATION)))
                .thenReturn(Optional.of(stored));
        service.verifyEmail("one-time-token");

        assertTrue(user.isEmailVerified());
        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
        assertNotNull(stored.getUsedAt());
    }
}
