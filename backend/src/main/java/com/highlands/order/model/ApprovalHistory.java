package com.highlands.order.model;

import com.highlands.order.domain.approval.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "approval_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_approval_history_request",
                columnNames = {"approval_id", "request_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_id", nullable = false)
    private OrderApproval approval;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 100)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ApprovalStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus toStatus;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 100)
    private String requestId;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
