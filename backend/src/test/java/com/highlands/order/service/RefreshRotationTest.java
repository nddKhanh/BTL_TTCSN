package com.highlands.order.service;

import com.highlands.order.model.*;
import com.highlands.order.repository.RefreshSessionRepository;
import com.highlands.order.repository.UserRepository;
import com.highlands.order.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshRotationTest {
    private final RefreshSessionRepository repository = mock(RefreshSessionRepository.class);
    private final JwtService jwt = mock(JwtService.class);
    private final RefreshSessionService service = new RefreshSessionService(repository, jwt, mock(UserRepository.class));

    private AppUser user() {
        return AppUser.builder().id(7L).email("person@example.com").role(Role.CUSTOMER)
                .accountStatus(AccountStatus.ACTIVE).build();
    }

    private RefreshSession session(AppUser user) {
        return RefreshSession.builder().user(user).familyId("family")
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .expiresAt(LocalDateTime.now().plusDays(1)).build();
    }

    @Test
    void rotationConsumesOldTokenAndStoresOnlyTheNewTokenHash() {
        var current = session(user());
        when(repository.findByTokenHash(RefreshSessionService.hash("old-token"))).thenReturn(Optional.of(current));
        when(jwt.createToken(current.getUser())).thenReturn("access");
        var result = service.rotate("old-token");
        var saved = org.mockito.ArgumentCaptor.forClass(RefreshSession.class);
        verify(repository).save(saved.capture());
        assertNotNull(current.getRevokedAt());
        assertEquals("family", saved.getValue().getFamilyId());
        assertEquals(RefreshSessionService.hash(result.refreshToken()), saved.getValue().getTokenHash());
        assertEquals(saved.getValue().getTokenHash(), current.getReplacedByHash());
        assertNotEquals(result.refreshToken(), saved.getValue().getTokenHash());
        assertEquals("access", result.accessToken());
    }

    @Test
    void replayRevokesActiveFamilyWithoutIssuingAnotherToken() {
        var old = session(user());
        old.setRevokedAt(LocalDateTime.now().minusMinutes(1));
        var replacement = session(old.getUser());
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(old));
        when(repository.findAllByFamilyIdAndRevokedAtIsNull("family")).thenReturn(List.of(replacement));
        assertThrows(BadCredentialsException.class, () -> service.rotate("old-token"));
        assertNotNull(replacement.getRevokedAt());
        verify(repository, never()).save(any());
        verifyNoInteractions(jwt);
    }
}
