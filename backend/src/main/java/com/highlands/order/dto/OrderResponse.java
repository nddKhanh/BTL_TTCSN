package com.highlands.order.dto;

import com.highlands.order.model.Order;
import com.highlands.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderCode,
        String customerName,
        String customerPhone,
        String deliveryAddress,
        String note,
        String couponCode,
        BigDecimal discountAmount,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getDeliveryAddress(),
                order.getNote(),
                order.getCouponCode(),
                order.getDiscountAmount(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .map(OrderItemResponse::from)
                        .toList()
        );
    }
}
