package com.highlands.order.service;

import com.highlands.order.application.approval.*;
import com.highlands.order.application.order.*;
import com.highlands.order.controller.OrderController;
import com.highlands.order.domain.approval.ApprovalActor;
import com.highlands.order.exception.GlobalExceptionHandler;
import com.highlands.order.model.*;
import com.highlands.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminOrderPagingTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final ApprovalPort approvals = mock(ApprovalPort.class);
    private final OrderQueryService service = new OrderQueryService(repository, approvals);
    private final ApprovalActor admin = new ApprovalActor(
            "user:1",
            1L,
            true,
            Set.of("ORDER_READ", "ORDER_APPROVE", "ORDER_FULFILL")
    );

    @Test
    void pagingNormalizesFiltersUsesExclusiveEndAndPreservesTotals() {
        var from = LocalDate.of(2026, 11, 1);
        var to = LocalDate.of(2026, 11, 4);
        var request = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var order = Order.builder().id(42L).orderCode("COF-EXAMPLE").items(new ArrayList<>()).build();
        when(repository.searchPageForAdmin(OrderStatus.COMPLETED, "COF", from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(), request)).thenReturn(new PageImpl<>(List.of(order), request, 45));
        when(approvals.findByOrderIds(List.of(42L))).thenReturn(Map.of());

        var page = service.getAllOrdersPage(" completed ", " COF ", from, to, 2, 10, admin);

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
                    () -> service.getAllOrdersPage(null, null, null, null, bounds[0], bounds[1], admin));
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.getAllOrdersPage("unknown", null, null, null, 0, 20, admin));
        assertThrows(IllegalArgumentException.class,
                () -> service.getAllOrdersPage(null, null, LocalDate.of(2026, 11, 4),
                        LocalDate.of(2026, 11, 3), 0, 20, admin));
        verifyNoInteractions(repository);
    }

    @Test
    void optionalBlankFiltersBecomeNullAndPageSizeIsBounded() {
        when(repository.searchPageForAdmin(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());
        assertTrue(service.getAllOrdersPage(" ", " ", null, null, 0, 100, admin).isEmpty());
        verify(repository).searchPageForAdmin(null, null, null, null,
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Test
    void httpPagingEndpointReturnsPageEnvelopeAndRejectsBadBounds() throws Exception {
        when(repository.searchPageForAdmin(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Order.builder().id(5L).items(new ArrayList<>()).build()),
                        PageRequest.of(0, 20), 1));
        when(approvals.findByOrderIds(anyCollection())).thenReturn(Map.of());
        WorkflowActorResolver actorResolver = mock(WorkflowActorResolver.class);
        when(actorResolver.resolve(nullable(UserDetails.class))).thenReturn(admin);
        var controller = new OrderController(
                mock(PlaceOrderService.class),
                service,
                mock(CancelOrderService.class),
                actorResolver
        );
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(get("/api/v1/orders/admin/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(5));
        mvc.perform(get("/api/v1/orders/admin/page").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }
}
