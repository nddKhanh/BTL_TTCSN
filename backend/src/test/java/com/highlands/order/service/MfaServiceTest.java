package com.highlands.order.service;

import com.highlands.order.model.AppUser;
import com.highlands.order.model.MfaEnrollment;
import com.highlands.order.repository.MfaEnrollmentRepository;
import com.highlands.order.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MfaServiceTest {
    @Test
    void confirmsTotpAndConsumesRecoveryCodeOnce() {
        MfaEnrollmentRepository enrollments = mock(MfaEnrollmentRepository.class);
        UserRepository users = mock(UserRepository.class);
        AppUser user = AppUser.builder().id(7L).email("user@example.com").build();
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(enrollments.save(any())).thenAnswer(call -> call.getArgument(0));
        MfaService service = new MfaService(enrollments, users, "test-encryption-key");

        MfaService.Setup setup = service.start(user.getEmail());
        MfaEnrollment stored = new MfaEnrollment();
        verify(enrollments).save(any());
        org.mockito.ArgumentCaptor<MfaEnrollment> captor = org.mockito.ArgumentCaptor.forClass(MfaEnrollment.class);
        verify(enrollments).save(captor.capture());
        stored = captor.getValue();
        when(enrollments.findByUserId(7L)).thenReturn(Optional.of(stored));

        String code = MfaService.totp(setup.secret(), System.currentTimeMillis() / 30_000L);
        var recovery = service.confirm(user.getEmail(), code);
        assertTrue(stored.isEnabled());
        assertEquals(8, recovery.size());

        service.challenge(user.getEmail(), recovery.get(0));
        assertThrows(IllegalArgumentException.class, () -> service.challenge(user.getEmail(), recovery.get(0)));
    }

    @Test
    void acceptsOnlyOneClockWindowOfSkew() {
        String secret = "JBSWY3DPEHPK3PXP";
        long current = System.currentTimeMillis() / 30_000L;
        assertTrue(MfaService.validTotp(secret, MfaService.totp(secret, current + 1)));
        assertFalse(MfaService.validTotp(secret, MfaService.totp(secret, current + 2)));
    }
}
