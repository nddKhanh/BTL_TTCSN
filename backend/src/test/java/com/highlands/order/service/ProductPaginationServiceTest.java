package com.highlands.order.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductPaginationServiceTest {
    @Test
    void defaultPageSizeIsExplicitlyRepresented() {
        assertEquals(12, PageRequest.of(0, 12).getPageSize());
    }
}
