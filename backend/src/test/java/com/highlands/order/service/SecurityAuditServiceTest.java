package com.highlands.order.service;

import com.highlands.order.model.SecurityAuditEvent;
import com.highlands.order.repository.SecurityAuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityAuditServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void returnsMaskedAndBoundedAuditFields() {
        SecurityAuditEventRepository repository = mock(SecurityAuditEventRepository.class);
        SecurityAuditEvent event = SecurityAuditEvent.builder().id(1L).eventType("LOGIN").result("SUCCESS")
                .actorEmail("person@example.com").ipAddress("192.168.10.22").userAgent("x".repeat(120))
                .details("Session issued").occurredAt(LocalDateTime.now()).build();
        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(event));

        var result = new SecurityAuditService(repository).search("login", "success", null, null).get(0);

        assertEquals("pe***@example.com", result.actorEmail());
        assertEquals("192.168.x.x", result.ipAddress());
        assertEquals(80, result.device().length());
        assertFalse(result.details().toLowerCase().contains("secret"));
    }
}
