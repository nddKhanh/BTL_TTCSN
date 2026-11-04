package com.highlands.order.service;

import com.highlands.order.config.OrderPricingProperties;
import com.highlands.order.model.AppUser;
import com.highlands.order.model.Order;
import com.highlands.order.repository.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderLookupBoundaryTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderService service = new OrderService(repository, mock(ProductRepository.class),
            mock(ToppingRepository.class), mock(UserRepository.class), mock(CouponService.class),
            new OrderPricingProperties(BigDecimal.ZERO, BigDecimal.ZERO));

    @Test
    void ownerCanLookUpAnOrderWithoutRepeatingTheirPhone() {
        var order = Order.builder().id(42L).orderCode("COF-EXAMPLE").customerPhone("0912345678")
                .user(AppUser.builder().email("owner@example.com").build()).build();
        when(repository.findByOrderCode("COF-EXAMPLE")).thenReturn(Optional.of(order));
        assertEquals(42L, service.getOrderByCode("COF-EXAMPLE", null, "OWNER@example.com").id());
        assertThrows(IllegalArgumentException.class,
                () -> service.getOrderByCode("COF-EXAMPLE", null, "other@example.com"));
        assertThrows(IllegalArgumentException.class, () -> service.getOrderByCode("COF-EXAMPLE", null));
    }
}
