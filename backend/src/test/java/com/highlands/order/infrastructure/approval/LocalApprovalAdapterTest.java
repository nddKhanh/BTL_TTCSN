package com.highlands.order.infrastructure.approval;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.domain.approval.*;
import com.highlands.order.model.*;
import com.highlands.order.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LocalApprovalAdapterTest {

    private final OrderApprovalRepository approvals = mock(OrderApprovalRepository.class);
    private final ApprovalHistoryRepository history = mock(ApprovalHistoryRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final LocalApprovalAdapter adapter = new LocalApprovalAdapter(approvals, history, orders);
    private OrderApproval approval;

    @BeforeEach
    void setUp() {
        approval = OrderApproval.builder()
                .id(5L)
                .approvalRef("approval-ref")
                .order(Order.builder().id(10L).build())
                .status(ApprovalStatus.PENDING)
                .createdBy("user:1")
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build();
    }

    @Test
    void executePersistsSingleAuthorizedDecision() {
        ApprovalActor approver = new ApprovalActor(
                "user:2",
                2L,
                true,
                Set.of("ORDER_READ", "ORDER_APPROVE")
        );
        when(approvals.findByApprovalRef("approval-ref")).thenReturn(Optional.of(approval));
        when(history.findByApproval_IdAndRequestId(5L, "request-1")).thenReturn(Optional.empty());
        when(approvals.saveAndFlush(approval)).thenReturn(approval);

        var result = adapter.execute(
                "approval-ref",
                "approve",
                approver,
                null,
                "request-1",
                0
        );

        assertEquals(ApprovalStatus.ACCEPTED, result.status());
        verify(history).save(argThat(item -> item.getAction().equals("APPROVE")
                && item.getFromStatus() == ApprovalStatus.PENDING
                && item.getToStatus() == ApprovalStatus.ACCEPTED));
    }

    @Test
    void creatorCannotApproveOwnRequest() {
        ApprovalActor creator = new ApprovalActor(
                "user:1",
                1L,
                true,
                Set.of("ORDER_READ", "ORDER_APPROVE")
        );
        when(approvals.findByApprovalRef("approval-ref")).thenReturn(Optional.of(approval));
        when(history.findByApproval_IdAndRequestId(5L, "request-2")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> adapter.execute(
                "approval-ref",
                "approve",
                creator,
                null,
                "request-2",
                0
        ));
        verify(approvals, never()).saveAndFlush(any());
    }

    @Test
    void replayWithSameRequestAndActionDoesNotRepeatEffect() {
        approval.setStatus(ApprovalStatus.ACCEPTED);
        ApprovalActor approver = new ApprovalActor(
                "user:2",
                2L,
                true,
                Set.of("ORDER_READ", "ORDER_APPROVE")
        );
        ApprovalHistory previous = ApprovalHistory.builder().action("APPROVE").build();
        when(approvals.findByApprovalRef("approval-ref")).thenReturn(Optional.of(approval));
        when(history.findByApproval_IdAndRequestId(5L, "request-1")).thenReturn(Optional.of(previous));

        var result = adapter.execute(
                "approval-ref",
                "approve",
                approver,
                null,
                "request-1",
                0
        );

        assertEquals(ApprovalStatus.ACCEPTED, result.status());
        verify(approvals, never()).saveAndFlush(any());
        verify(history, never()).save(any());
    }

    @Test
    void actionMetadataDescribesReasonRequirement() {
        ApprovalActor approver = new ApprovalActor(
                "user:2",
                2L,
                true,
                Set.of("ORDER_READ", "ORDER_APPROVE")
        );
        ApprovalPort.Snapshot snapshot = new ApprovalPort.Snapshot(
                "approval-ref",
                10L,
                ApprovalStatus.PENDING,
                "pending",
                "user:1",
                null,
                LocalDateTime.now(),
                null,
                0
        );

        var metadata = adapter.availableActionDetails(snapshot, approver);

        assertTrue(metadata.stream().anyMatch(action -> action.name().equals("approve")
                && action.label().equals("Chấp nhận")
                && !action.reasonRequired()));
        assertTrue(metadata.stream().anyMatch(action -> action.name().equals("reject")
                && action.reasonRequired()));
    }
}
