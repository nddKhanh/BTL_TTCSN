package com.highlands.order.service;

import com.highlands.order.application.approval.ApprovalApplicationService;
import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.application.order.OrderPricingService;
import com.highlands.order.application.order.PlaceOrderService;
import com.highlands.order.config.OrderPricingProperties;
import com.highlands.order.domain.approval.*;
import com.highlands.order.domain.fulfillment.FulfillmentStatus;
import com.highlands.order.dto.CreateOrderRequest;
import com.highlands.order.dto.OrderItemRequest;
import com.highlands.order.model.*;
import com.highlands.order.repository.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderApplicationServicesTest {

    @Test
    void placeOrder_attachesAuthenticatedOwnerAndSubmitsApproval() {
        OrderRepository orders = mock(OrderRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        ToppingRepository toppings = mock(ToppingRepository.class);
        UserRepository users = mock(UserRepository.class);
        CouponService coupons = mock(CouponService.class);
        ApprovalPort approvals = mock(ApprovalPort.class);
        var pricing = new OrderPricingService(new OrderPricingProperties(
                BigDecimal.valueOf(15000),
                BigDecimal.valueOf(200000)
        ));
        var service = new PlaceOrderService(orders, products, toppings, users, coupons, pricing, approvals);

        AppUser customer = AppUser.builder().id(7L).email("customer@example.com").build();
        Product product = Product.builder()
                .id(1L)
                .name("Phin Sữa Đá")
                .basePrice(BigDecimal.valueOf(29000))
                .sizes(new ArrayList<>())
                .build();
        product.getSizes().add(ProductSize.builder()
                .product(product)
                .sizeName("S")
                .extraPrice(BigDecimal.ZERO)
                .build());
        CreateOrderRequest request = new CreateOrderRequest(
                "Khách hàng",
                "0900000000",
                "123 Đường A",
                null,
                null,
                List.of(new OrderItemRequest(1L, "S", List.of(), null, null, null, 1))
        );
        ApprovalActor actor = new ApprovalActor(
                "user:7",
                7L,
                true,
                Set.of("ORDER_READ_OWN")
        );

        when(toppings.findAll()).thenReturn(List.of());
        when(products.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(product));
        when(users.findById(7L)).thenReturn(Optional.of(customer));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(10L);
            order.setVersion(0L);
            return order;
        });
        when(approvals.submit(any(), eq(actor), anyString())).thenAnswer(invocation -> new ApprovalPort.Snapshot(
                "approval-ref",
                10L,
                ApprovalStatus.PENDING,
                "pending",
                actor.actorId(),
                null,
                LocalDateTime.now(),
                null,
                0
        ));
        when(approvals.availableActionDetails(any(), eq(actor))).thenReturn(List.of());

        service.place(request, actor);

        var orderCaptor = org.mockito.ArgumentCaptor.forClass(Order.class);
        verify(orders).save(orderCaptor.capture());
        assertSame(customer, orderCaptor.getValue().getUser());
        assertEquals(FulfillmentStatus.NOT_STARTED, orderCaptor.getValue().getFulfillmentStatus());
        verify(approvals).submit(any(), eq(actor), startsWith("submit:COF-"));
    }

    @Test
    void acceptedApprovalUpdatesOnlyCompatibilityStatus() {
        OrderRepository orders = mock(OrderRepository.class);
        ApprovalPort approvals = mock(ApprovalPort.class);
        var service = new ApprovalApplicationService(approvals, orders);
        ApprovalActor actor = new ApprovalActor("user:2", 2L, true, Set.of("ORDER_READ", "ORDER_APPROVE"));
        Order order = Order.builder()
                .id(10L)
                .status(OrderStatus.PENDING)
                .fulfillmentStatus(FulfillmentStatus.NOT_STARTED)
                .items(new ArrayList<>())
                .build();
        ApprovalPort.Snapshot pending = snapshot(ApprovalStatus.PENDING, 0);
        ApprovalPort.Snapshot accepted = snapshot(ApprovalStatus.ACCEPTED, 1);
        when(orders.findById(10L)).thenReturn(Optional.of(order));
        when(approvals.getByOrderId(10L, actor)).thenReturn(pending);
        when(approvals.execute("approval-ref", "approve", actor, null, "request-1", 0))
                .thenReturn(accepted);
        when(approvals.availableActionDetails(accepted, actor)).thenReturn(List.of());
        when(orders.saveAndFlush(order)).thenReturn(order);

        service.execute(10L, "approve", actor, null, "request-1", 0);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(FulfillmentStatus.NOT_STARTED, order.getFulfillmentStatus());
    }

    @Test
    void rejectedApprovalClosesFulfillmentWithoutSuccess() {
        OrderRepository orders = mock(OrderRepository.class);
        ApprovalPort approvals = mock(ApprovalPort.class);
        var service = new ApprovalApplicationService(approvals, orders);
        ApprovalActor actor = new ApprovalActor("user:2", 2L, true, Set.of("ORDER_READ", "ORDER_APPROVE"));
        Order order = Order.builder()
                .id(10L)
                .status(OrderStatus.PENDING)
                .fulfillmentStatus(FulfillmentStatus.NOT_STARTED)
                .items(new ArrayList<>())
                .build();
        when(orders.findById(10L)).thenReturn(Optional.of(order));
        when(approvals.getByOrderId(10L, actor)).thenReturn(snapshot(ApprovalStatus.PENDING, 0));
        when(approvals.execute(anyString(), eq("reject"), eq(actor), eq("Hết hàng"), anyString(), eq(0L)))
                .thenReturn(snapshot(ApprovalStatus.REJECTED, 1));
        when(approvals.availableActionDetails(any(), eq(actor))).thenReturn(List.of());
        when(orders.saveAndFlush(order)).thenReturn(order);

        service.execute(10L, "reject", actor, "Hết hàng", "request-2", 0);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(FulfillmentStatus.CANCELLED, order.getFulfillmentStatus());
    }

    private ApprovalPort.Snapshot snapshot(ApprovalStatus status, long version) {
        return new ApprovalPort.Snapshot(
                "approval-ref",
                10L,
                status,
                status.name().toLowerCase(Locale.ROOT),
                "user:1",
                null,
                LocalDateTime.now(),
                null,
                version
        );
    }
}
