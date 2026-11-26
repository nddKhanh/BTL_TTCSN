package com.highlands.order.service;

import com.highlands.order.model.SecurityAuditEvent;
import com.highlands.order.repository.SecurityAuditEventRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityAuditListenerTest {
    @Test
    void persistsOnlyBoundedLoginMetadata() {
        SecurityAuditEventRepository repository = mock(SecurityAuditEventRepository.class);
        var listener = new SecurityAuditListener(repository);
        listener.recordSuccessfulLogin(new SuccessfulLoginEvent("user@example.com", LocalDateTime.now(),
                "10.0.0.1\r\ninjected", "x".repeat(250)));

        var capture = org.mockito.ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(repository).save(capture.capture());
        SecurityAuditEvent saved = capture.getValue();
        assertEquals("LOGIN", saved.getEventType());
        assertFalse(saved.getIpAddress().contains("\n"));
        assertEquals(200, saved.getUserAgent().length());
        assertFalse(saved.getDetails().toLowerCase().contains("token"));
    }
}
