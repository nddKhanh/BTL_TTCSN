package com.highlands.order.service;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.application.order.OrderQueryService;
import com.highlands.order.domain.approval.*;
import com.highlands.order.exception.ApiDomainException;
import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.AppUser;
import com.highlands.order.model.Order;
import com.highlands.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderLookupBoundaryTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final ApprovalPort approvals = mock(ApprovalPort.class);
    private final OrderQueryService service = new OrderQueryService(repository, approvals);

    @Test
    void ownerCanLookUpAnOrderWithoutRepeatingTheirPhone() {
        var order = Order.builder().id(42L).orderCode("COF-EXAMPLE").customerPhone("0912345678")
                .user(AppUser.builder().id(7L).email("owner@example.com").build()).build();
        when(repository.findByOrderCode("COF-EXAMPLE")).thenReturn(Optional.of(order));
        when(approvals.findByOrderIds(List.of(42L))).thenReturn(Map.of(42L, snapshot(42L, "user:7")));
        ApprovalActor owner = new ApprovalActor("user:7", 7L, true, Set.of("ORDER_READ_OWN"));
        ApprovalActor other = new ApprovalActor("user:8", 8L, true, Set.of("ORDER_READ_OWN"));

        assertEquals(42L, service.getByCode("COF-EXAMPLE", null, owner).id());
        assertThrows(ApiDomainException.class, () -> service.getByCode("COF-EXAMPLE", null, other));
        assertThrows(ApiDomainException.class,
                () -> service.getByCode("COF-EXAMPLE", null, ApprovalActor.anonymous()));
    }

    @Test
    void guestAndOtherAccountsStillNeedTheExactPhone() {
        var order = Order.builder().id(8L).orderCode("COF-GUEST").customerPhone("0912345678").build();
        when(repository.findByOrderCode("COF-GUEST")).thenReturn(Optional.of(order));
        when(approvals.findByOrderIds(List.of(8L))).thenReturn(Map.of(8L, snapshot(8L, "guest-order:COF-GUEST")));
        ApprovalActor other = new ApprovalActor("user:8", 8L, true, Set.of("ORDER_READ_OWN"));

        assertEquals(8L, service.getByCode("COF-GUEST", " 0912345678 ", ApprovalActor.anonymous()).id());
        assertEquals(8L, service.getByCode("COF-GUEST", "0912345678", other).id());
        for (String phone : new String[]{null, "", " ", "091234567", "0912345670"}) {
            assertThrows(ApiDomainException.class, () -> service.getByCode("COF-GUEST", phone, other));
        }
    }

    @Test
    void missingOrdersRemainNotFound() {
        when(repository.findByOrderCode("COF-MISSING")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.getByCode("COF-MISSING", "0912345678", ApprovalActor.anonymous()));
    }

    private ApprovalPort.Snapshot snapshot(Long orderId, String owner) {
        return new ApprovalPort.Snapshot(
                "ref-" + orderId,
                orderId,
                ApprovalStatus.PENDING,
                "pending",
                owner,
                null,
                LocalDateTime.now(),
                null,
                0
        );
    }
}
