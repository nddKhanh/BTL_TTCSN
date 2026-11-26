package com.highlands.order.service;

import com.highlands.order.model.SecurityAuditEvent;
import com.highlands.order.repository.SecurityAuditEventRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SecurityAuditListener {
    private final SecurityAuditEventRepository events;

    public SecurityAuditListener(SecurityAuditEventRepository events) {
        this.events = events;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void recordSuccessfulLogin(SuccessfulLoginEvent event) {
        events.save(SecurityAuditEvent.builder()
                .eventType("LOGIN")
                .result("SUCCESS")
                .actorEmail(event.email())
                .ipAddress(compact(event.ipAddress(), 64))
                .userAgent(compact(event.userAgent(), 200))
                .details("Session issued")
                .occurredAt(event.occurredAt())
                .build());
    }

    private static String compact(String value, int max) {
        if (value == null) return null;
        String clean = value.replaceAll("[\\r\\n]", " ");
        return clean.substring(0, Math.min(clean.length(), max));
    }
}
