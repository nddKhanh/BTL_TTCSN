package com.highlands.order.dto;

import com.highlands.order.model.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String sizeName,
        String toppings,
        String iceLevel,
        String sugarLevel,
        String note,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getSizeName(),
                item.getToppings(),
                item.getIceLevel(),
                item.getSugarLevel(),
                item.getNote(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}
