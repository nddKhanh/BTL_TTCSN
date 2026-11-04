package com.highlands.order.service;

import com.highlands.order.model.SecurityAuditEvent;
import com.highlands.order.repository.SecurityAuditEventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SecurityAuditService {
    private final SecurityAuditEventRepository events;

    public SecurityAuditService(SecurityAuditEventRepository events) {
        this.events = events;
    }

    @PreAuthorize("hasAuthority('REPORT_READ')")
    public List<AuditView> search(String eventType, String result, LocalDateTime from, LocalDateTime to) {
        Specification<SecurityAuditEvent> filter = (root, query, cb) -> cb.conjunction();
        if (eventType != null && !eventType.isBlank())
            filter = filter.and((root, query, cb) -> cb.equal(root.get("eventType"), eventType.trim().toUpperCase()));
        if (result != null && !result.isBlank())
            filter = filter.and((root, query, cb) -> cb.equal(root.get("result"), result.trim().toUpperCase()));
        if (from != null) filter = filter.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
        if (to != null) filter = filter.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to));
        return events.findAll(filter, Sort.by(Sort.Direction.DESC, "occurredAt")).stream()
                .map(SecurityAuditService::sanitize).toList();
    }

    static AuditView sanitize(SecurityAuditEvent event) {
        return new AuditView(event.getId(), event.getEventType(), event.getResult(), maskEmail(event.getActorEmail()),
                maskIp(event.getIpAddress()), compact(event.getUserAgent(), 80), event.getDetails(), event.getOccurredAt());
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "ẩn";
        int at = email.indexOf('@');
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }

    private static String maskIp(String ip) {
        if (ip == null) return "ẩn";
        String[] parts = ip.split("\\.");
        return parts.length == 4 ? parts[0] + "." + parts[1] + ".x.x" : "ẩn";
    }

    private static String compact(String value, int max) {
        if (value == null) return null;
        return value.substring(0, Math.min(value.length(), max));
    }

    public record AuditView(Long id, String eventType, String result, String actorEmail,
                            String ipAddress, String device, String details, LocalDateTime occurredAt) { }
}
