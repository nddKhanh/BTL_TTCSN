package com.highlands.order.service;

import com.highlands.order.config.OrderPricingProperties;
import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.Order;
import com.highlands.order.model.OrderStatus;
import com.highlands.order.repository.OrderRepository;
import com.highlands.order.repository.ProductRepository;
import com.highlands.order.repository.ToppingRepository;
import com.highlands.order.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ToppingRepository toppingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CouponService couponService;
    @Mock
    private OrderPricingProperties pricing;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                productRepository,
                toppingRepository,
                userRepository,
                couponService,
                pricing
        );
    }

    @Test
    void cancelMyOrder_pendingOwnedOrder_cancelsOrder() {
        Order order = Order.builder()
                .id(10L)
                .status(OrderStatus.PENDING)
                .build();
        when(orderRepository.findByIdAndUserEmailIgnoreCase(10L, "customer@example.com"))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.cancelMyOrder(10L, "customer@example.com");

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void cancelMyOrder_confirmedOrder_rejectsCancellation() {
        Order order = Order.builder()
                .id(11L)
                .status(OrderStatus.CONFIRMED)
                .build();
        when(orderRepository.findByIdAndUserEmailIgnoreCase(11L, "customer@example.com"))
                .thenReturn(Optional.of(order));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.cancelMyOrder(11L, "customer@example.com")
        );

        assertEquals("Chỉ có thể hủy đơn đang chờ xác nhận", exception.getMessage());
    }

    @Test
    void cancelMyOrder_orderDoesNotBelongToUser_returnsNotFound() {
        when(orderRepository.findByIdAndUserEmailIgnoreCase(12L, "other@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.cancelMyOrder(12L, "other@example.com")
        );
    }

    @Test
    void getAllOrders_filtersAndMakesEndDateInclusive() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 8);
        when(orderRepository.searchForAdmin(
                OrderStatus.PENDING,
                "Lan",
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 9, 0, 0)
        )).thenReturn(List.of());

        orderService.getAllOrders("pending", "  Lan  ", from, to);

        verify(orderRepository).searchForAdmin(
                OrderStatus.PENDING,
                "Lan",
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 9, 0, 0)
        );
    }

    @Test
    void getAllOrders_invalidDateRange_rejectsBeforeQuery() {
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.getAllOrders(
                        null,
                        null,
                        LocalDate.of(2026, 10, 9),
                        LocalDate.of(2026, 10, 8)
                )
        );

        verifyNoInteractions(orderRepository);
    }
}
