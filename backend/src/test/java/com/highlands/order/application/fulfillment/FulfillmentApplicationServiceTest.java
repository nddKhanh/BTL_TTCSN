package com.highlands.order.application.fulfillment;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.domain.approval.*;
import com.highlands.order.domain.fulfillment.*;
import com.highlands.order.exception.ApiDomainException;
import com.highlands.order.model.Order;
import com.highlands.order.model.OrderStatus;
import com.highlands.order.repository.FulfillmentHistoryRepository;
import com.highlands.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FulfillmentApplicationServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final FulfillmentHistoryRepository history = mock(FulfillmentHistoryRepository.class);
    private final ApprovalPort approvals = mock(ApprovalPort.class);
    private final FulfillmentApplicationService service = new FulfillmentApplicationService(orders, history, approvals);
    private final ApprovalActor staff = new ApprovalActor(
            "user:2",
            2L,
            true,
            Set.of("ORDER_READ", "ORDER_FULFILL")
    );

    @Test
    void startsPreparationOnlyAfterAcceptance() {
        Order order = Order.builder()
                .id(10L)
                .status(OrderStatus.CONFIRMED)
                .fulfillmentStatus(FulfillmentStatus.NOT_STARTED)
                .version(0L)
                .items(new ArrayList<>())
                .build();
        when(orders.findById(10L)).thenReturn(Optional.of(order));
        when(approvals.getByOrderId(10L, staff)).thenReturn(snapshot(ApprovalStatus.ACCEPTED));
        when(history.findByOrder_IdAndRequestId(10L, "request-1")).thenReturn(Optional.empty());
        when(orders.saveAndFlush(order)).thenReturn(order);

        service.execute(10L, FulfillmentAction.START_PREPARATION, staff, null, "request-1", 0);

        assertEquals(FulfillmentStatus.PREPARING, order.getFulfillmentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(history).save(argThat(item -> item.getAction().equals("START_PREPARATION")));
    }

    @Test
    void rejectsStaleVersionBeforeChangingOrder() {
        Order order = Order.builder()
                .id(10L)
                .status(OrderStatus.CONFIRMED)
                .fulfillmentStatus(FulfillmentStatus.NOT_STARTED)
                .version(2L)
                .items(new ArrayList<>())
                .build();
        when(orders.findById(10L)).thenReturn(Optional.of(order));
        when(approvals.getByOrderId(10L, staff)).thenReturn(snapshot(ApprovalStatus.ACCEPTED));
        when(history.findByOrder_IdAndRequestId(10L, "request-2")).thenReturn(Optional.empty());

        assertThrows(ApiDomainException.class, () -> service.execute(
                10L,
                FulfillmentAction.START_PREPARATION,
                staff,
                null,
                "request-2",
                1
        ));
        verify(orders, never()).saveAndFlush(any());
    }

    private ApprovalPort.Snapshot snapshot(ApprovalStatus status) {
        return new ApprovalPort.Snapshot(
                "approval-ref",
                10L,
                status,
                status.name().toLowerCase(Locale.ROOT),
                "user:1",
                null,
                LocalDateTime.now(),
                null,
                1
        );
    }
}
