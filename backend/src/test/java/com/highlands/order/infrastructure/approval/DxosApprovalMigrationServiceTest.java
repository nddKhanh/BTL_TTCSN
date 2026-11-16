package com.highlands.order.infrastructure.approval;

import com.highlands.order.domain.approval.ApprovalProvider;
import com.highlands.order.domain.approval.ApprovalStatus;
import com.highlands.order.model.Order;
import com.highlands.order.model.OrderApproval;
import com.highlands.order.repository.ApprovalHistoryRepository;
import com.highlands.order.repository.OrderApprovalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DxosApprovalMigrationServiceTest {

    @Test
    void migratesAcceptedApprovalThroughMetadataPathAndSkipsItOnRerun() {
        DxosApiClient client = mock(DxosApiClient.class);
        OrderApprovalRepository approvals = mock(OrderApprovalRepository.class);
        ApprovalHistoryRepository history = mock(ApprovalHistoryRepository.class);
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());

        Order order = Order.builder()
                .id(7L)
                .orderCode("ORD-007")
                .customerName("Demo User")
                .totalAmount(new BigDecimal("125000"))
                .build();
        OrderApproval approval = OrderApproval.builder()
                .id(11L)
                .approvalRef("local-ref")
                .provider(ApprovalProvider.LOCAL)
                .order(order)
                .status(ApprovalStatus.ACCEPTED)
                .createdBy("user:7")
                .build();

        when(approvals.findByProviderOrderById(ApprovalProvider.LOCAL))
                .thenAnswer(invocation -> approval.getProvider() == ApprovalProvider.LOCAL
                        ? List.of(approval) : List.of());
        when(approvals.findByProviderAndLocalApprovalRefIsNotNullAndMigrationCompletedAtIsNullOrderById(
                ApprovalProvider.DXOS))
                .thenAnswer(invocation -> approval.getProvider() == ApprovalProvider.DXOS
                        && approval.getLocalApprovalRef() != null
                        && approval.getMigrationCompletedAt() == null ? List.of(approval) : List.of());
        when(approvals.findById(11L)).thenReturn(Optional.of(approval));
        when(approvals.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvals.countByProvider(any())).thenAnswer(invocation ->
                approval.getProvider() == invocation.getArgument(0) ? 1L : 0L);
        when(history.findByApproval_IdAndRequestId(11L, "dxos-migration:11"))
                .thenReturn(Optional.empty());

        DxosApiClient.EntityMetadata metadata = metadata();
        when(client.metadata(DxosApiClient.RemoteIdentity.APPROVER)).thenReturn(metadata);
        when(client.create(any())).thenReturn(record("91", "pending", 0));
        when(client.act("91", "start_review", "Chuyển đổi từ TTCSN Local sang DX-OS",
                DxosApiClient.RemoteIdentity.APPROVER))
                .thenReturn(record("91", "reviewing", 1));
        when(client.act("91", "approve", "Chuyển đổi từ TTCSN Local sang DX-OS",
                DxosApiClient.RemoteIdentity.APPROVER))
                .thenReturn(record("91", "accepted", 2));

        DxosApprovalMigrationService service = new DxosApprovalMigrationService(
                client, approvals, history, transactions);

        var first = service.migrate();
        var second = service.migrate();

        assertEquals(1, first.migrated());
        assertEquals(0, first.failed());
        assertEquals(0, second.eligible());
        assertEquals(ApprovalProvider.DXOS, approval.getProvider());
        assertEquals("local-ref", approval.getLocalApprovalRef());
        assertEquals("91", approval.getApprovalRef());
        assertNotNull(approval.getMigrationCompletedAt());
        verify(client, times(1)).create(any());
        verify(history, times(1)).save(any());
    }

    @Test
    void preflightReportsMissingPathWithoutCreatingRemoteData() {
        DxosApiClient client = mock(DxosApiClient.class);
        OrderApprovalRepository approvals = mock(OrderApprovalRepository.class);
        ApprovalHistoryRepository history = mock(ApprovalHistoryRepository.class);
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        Order order = Order.builder().id(8L).orderCode("ORD-008")
                .customerName("Demo").totalAmount(BigDecimal.TEN).build();
        OrderApproval approval = OrderApproval.builder().id(12L).approvalRef("local-2")
                .provider(ApprovalProvider.LOCAL).order(order).status(ApprovalStatus.REJECTED).build();
        when(approvals.findByProviderOrderById(ApprovalProvider.LOCAL)).thenReturn(List.of(approval));
        when(approvals.findByProviderAndLocalApprovalRefIsNotNullAndMigrationCompletedAtIsNullOrderById(
                ApprovalProvider.DXOS)).thenReturn(List.of());
        when(client.metadata(DxosApiClient.RemoteIdentity.APPROVER)).thenReturn(new DxosApiClient.EntityMetadata(
                new DxosApiClient.ProcessMetadata(List.of(), List.of()), List.of()));

        var report = new DxosApprovalMigrationService(client, approvals, history, transactions).preflight();

        assertEquals(1, report.eligible());
        assertFalse(report.issues().isEmpty());
        verify(client, never()).create(any());
    }

    private DxosApiClient.EntityMetadata metadata() {
        return new DxosApiClient.EntityMetadata(
                new DxosApiClient.ProcessMetadata(
                        List.of(),
                        List.of(
                                new DxosApiClient.ActionMetadata(
                                        "start_review", "Tiếp nhận", List.of("pending"),
                                        "reviewing", "HANDLER", false),
                                new DxosApiClient.ActionMetadata(
                                        "approve", "Duyệt", List.of("reviewing"),
                                        "accepted", "HANDLER", false),
                                new DxosApiClient.ActionMetadata(
                                        "reject", "Từ chối", List.of("reviewing"),
                                        "rejected", "HANDLER", true),
                                new DxosApiClient.ActionMetadata(
                                        "withdraw", "Rút", List.of("pending", "reviewing"),
                                        "withdrawn", "OWNER", false)
                        )
                ),
                List.of("start_review", "approve", "reject")
        );
    }

    private DxosApiClient.RecordData record(String id, String status, long version) {
        return new DxosApiClient.RecordData(
                id, status, version, "ttcsn-requester", Instant.now(), null, null);
    }
}
