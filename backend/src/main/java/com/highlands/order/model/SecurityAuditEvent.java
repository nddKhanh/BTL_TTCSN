package com.highlands.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_audit_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SecurityAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(length = 150)
    private String actorEmail;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 200)
    private String userAgent;

    @Column(length = 500)
    private String details;

    @Column(nullable = false)
    private LocalDateTime occurredAt;
}
