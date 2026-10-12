package com.highlands.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatsResponse(
    BigDecimal totalRevenue,
    long totalOrders,
    long pendingOrders,
    long completedOrders,
    long cancelledOrders,
    long totalProducts,
    long totalUsers,
    List<OrderResponse> recentOrders
) {}
