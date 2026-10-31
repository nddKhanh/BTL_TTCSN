package com.highlands.order.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductPaginationTest {
    @Test
    void pageRequestCarriesStableSortAndBounds() {
        PageRequest request = PageRequest.of(2, 12, Sort.by(Sort.Direction.DESC, "basePrice"));
        assertEquals(2, request.getPageNumber());
        assertEquals(12, request.getPageSize());
        assertEquals("basePrice", request.getSort().getOrderFor("basePrice").getProperty());
    }
}
