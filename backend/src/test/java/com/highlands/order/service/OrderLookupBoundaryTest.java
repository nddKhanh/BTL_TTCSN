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

    @Test
    void guestAndOtherAccountsStillNeedTheExactPhone() {
        var order = Order.builder().id(8L).orderCode("COF-GUEST").customerPhone("0912345678").build();
        when(repository.findByOrderCode("COF-GUEST")).thenReturn(Optional.of(order));
        assertEquals(8L, service.getOrderByCode("COF-GUEST", " 0912345678 ", null).id());
        assertEquals(8L, service.getOrderByCode("COF-GUEST", "0912345678", "other@example.com").id());
        for (String phone : new String[]{null, "", " ", "091234567", "0912345670"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.getOrderByCode("COF-GUEST", phone, "other@example.com"));
        }
    }

    @Test
    void emailCannotClaimAnUnownedGuestOrderAndMissingOrdersRemainNotFound() {
        var order = Order.builder().id(9L).orderCode("COF-UNOWNED").customerPhone("0912345678").build();
        when(repository.findByOrderCode("COF-UNOWNED")).thenReturn(Optional.of(order));
        assertThrows(IllegalArgumentException.class,
                () -> service.getOrderByCode("COF-UNOWNED", null, "owner@example.com"));
        when(repository.findByOrderCode("COF-MISSING")).thenReturn(Optional.empty());
        assertThrows(com.highlands.order.exception.ResourceNotFoundException.class,
                () -> service.getOrderByCode("COF-MISSING", "0912345678", "owner@example.com"));
    }
}
