package com.highlands.order.service;

import com.highlands.order.model.SecurityAuditEvent;
import com.highlands.order.repository.SecurityAuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditPaginationTest {
    private final SecurityAuditEventRepository repository = mock(SecurityAuditEventRepository.class);
    private final SecurityAuditService service = new SecurityAuditService(repository);

    @Test
    @SuppressWarnings("unchecked")
    void preservesPageMetadataAndSanitizesItsContent() {
        var event = SecurityAuditEvent.builder().id(42L).eventType("LOGIN").result("SUCCESS")
                .actorEmail("person@example.com").ipAddress("10.1.2.3").userAgent("browser").build();
        var request = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "occurredAt", "id"));
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), request, 45));
        var page = service.searchPage("login", "success", null, null, 2, 10);
        var captured = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captured.capture());
        assertEquals(request, captured.getValue());
        assertEquals(45, page.getTotalElements());
        assertEquals(2, page.getNumber());
        assertEquals("pe***@example.com", page.getContent().get(0).actorEmail());
        assertEquals("10.1.x.x", page.getContent().get(0).ipAddress());
    }

    @Test
    void rejectsInvalidPagingAndReversedDatesBeforeQuerying() {
        for (int[] bounds : new int[][]{{-1, 20}, {0, 0}, {0, 101}}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchPage(null, null, null, null, bounds[0], bounds[1]));
        }
        var now = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class,
                () -> service.searchPage(null, null, now, now.minusSeconds(1), 0, 20));
        verifyNoInteractions(repository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void legacyListIsBoundedToOneHundredEvents() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<SecurityAuditEvent>(List.of()));
        assertTrue(service.search(null, null, null, null).isEmpty());
        var captured = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captured.capture());
        assertEquals(0, captured.getValue().getPageNumber());
        assertEquals(100, captured.getValue().getPageSize());
    }
}
