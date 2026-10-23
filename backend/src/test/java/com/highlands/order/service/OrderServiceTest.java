package com.highlands.order.service;

import com.highlands.order.config.OrderPricingProperties;
import com.highlands.order.dto.CreateOrderRequest;
import com.highlands.order.dto.OrderItemRequest;
import com.highlands.order.dto.OrderResponse;
import com.highlands.order.exception.ApiDomainException;
import com.highlands.order.exception.ApiErrorCode;
import com.highlands.order.model.AppUser;
import com.highlands.order.model.Order;
import com.highlands.order.model.OrderStatus;
import com.highlands.order.model.Product;
import com.highlands.order.model.ProductSize;
import com.highlands.order.repository.OrderRepository;
import com.highlands.order.repository.ProductRepository;
import com.highlands.order.repository.ToppingRepository;
import com.highlands.order.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

        OrderResponse result = orderService.cancelMyOrder(10L, "customer@example.com");

        assertEquals(OrderStatus.CANCELLED, result.status());
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

        ApiDomainException exception = assertThrows(
                ApiDomainException.class,
                () -> orderService.cancelMyOrder(11L, "customer@example.com")
        );

        assertEquals(ApiErrorCode.ORDER_STATE_INVALID, exception.getErrorCode());
        assertEquals("Chỉ có thể hủy đơn đang chờ xác nhận", exception.getMessage());
    }

    @Test
    void cancelMyOrder_orderDoesNotBelongToUser_returnsNotFound() {
        when(orderRepository.findByIdAndUserEmailIgnoreCase(12L, "other@example.com"))
                .thenReturn(Optional.empty());

        ApiDomainException exception = assertThrows(
                ApiDomainException.class,
                () -> orderService.cancelMyOrder(12L, "other@example.com")
        );

        assertEquals(ApiErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
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

    @Test
    void createOrder_authenticatedCustomer_attachesOrderToAccount() {
        AppUser customer = AppUser.builder().email("customer@example.com").build();
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

        when(toppingRepository.findAll()).thenReturn(List.of());
        when(productRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(product));
        when(pricing.freeShippingThreshold()).thenReturn(BigDecimal.valueOf(200000));
        when(pricing.shippingFee()).thenReturn(BigDecimal.valueOf(15000));
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.createOrder(request, "customer@example.com");

        var orderCaptor = org.mockito.ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertSame(customer, orderCaptor.getValue().getUser());
    }

    @Test
    void getMyOrders_matchesEmailIgnoringCase() {
        when(orderRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc("Customer@Example.com"))
                .thenReturn(List.of());

        orderService.getMyOrders("Customer@Example.com");

        verify(orderRepository).findByUserEmailIgnoreCaseOrderByCreatedAtDesc("Customer@Example.com");
    }

    @Test
    void getOrderByCode_unknownCode_returnsNotFound() {
        when(orderRepository.findByOrderCode("COF-MISSING"))
                .thenReturn(Optional.empty());

        assertThrows(
                ApiDomainException.class,
                () -> orderService.getOrderByCode("COF-MISSING")
        );
    }
}
