package com.highlands.order.service;

import com.highlands.order.config.OrderPricingProperties;
import com.highlands.order.controller.OrderController;
import com.highlands.order.exception.GlobalExceptionHandler;
import com.highlands.order.model.*;
import com.highlands.order.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminOrderPagingTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderService service = new OrderService(repository, mock(ProductRepository.class),
            mock(ToppingRepository.class), mock(UserRepository.class), mock(CouponService.class),
            new OrderPricingProperties(BigDecimal.ZERO, BigDecimal.ZERO));

    @Test
    void pagingNormalizesFiltersUsesExclusiveEndAndPreservesTotals() {
        var from = LocalDate.of(2026, 11, 1);
        var to = LocalDate.of(2026, 11, 4);
        var request = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var order = Order.builder().id(42L).orderCode("COF-EXAMPLE").build();
        when(repository.searchPageForAdmin(OrderStatus.COMPLETED, "COF", from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(), request)).thenReturn(new PageImpl<>(List.of(order), request, 45));
        var page = service.getAllOrdersPage(" completed ", " COF ", from, to, 2, 10);
        assertEquals(45, page.getTotalElements());
        assertEquals(5, page.getTotalPages());
        assertEquals(2, page.getNumber());
        assertEquals(42L, page.getContent().get(0).id());
        verify(repository, never()).searchForAdmin(any(), any(), any(), any());
    }

    @Test
    void invalidBoundsStatusOrDatesAreRejectedBeforeTheRepository() {
        for (int[] bounds : new int[][]{{-1, 20}, {0, 0}, {0, 101}}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.getAllOrdersPage(null, null, null, null, bounds[0], bounds[1]));
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.getAllOrdersPage("unknown", null, null, null, 0, 20));
        assertThrows(IllegalArgumentException.class,
                () -> service.getAllOrdersPage(null, null, LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 3), 0, 20));
        verifyNoInteractions(repository);
    }

    @Test
    void optionalBlankFiltersBecomeNullAndPageSizeIsBounded() {
        when(repository.searchPageForAdmin(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());
        assertTrue(service.getAllOrdersPage(" ", " ", null, null, 0, 100).isEmpty());
        verify(repository).searchPageForAdmin(null, null, null, null,
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Test
    void httpPagingEndpointReturnsPageEnvelopeAndRejectsBadBounds() throws Exception {
        when(repository.searchPageForAdmin(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Order.builder().id(5L).build()), PageRequest.of(0, 20), 1));
        var mvc = MockMvcBuilders.standaloneSetup(new OrderController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/v1/orders/admin/page"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(5));
        mvc.perform(get("/api/v1/orders/admin/page").param("page", "-1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }
}
