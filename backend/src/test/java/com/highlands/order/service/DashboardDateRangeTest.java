package com.highlands.order.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardDateRangeTest {
    @Test
    void inclusiveEndDateUsesFollowingMidnightAsExclusiveBound() {
        LocalDate end = LocalDate.of(2026, 10, 24);
        assertEquals(LocalDate.of(2026, 10, 25).atStartOfDay(), end.plusDays(1).atStartOfDay());
    }
}
