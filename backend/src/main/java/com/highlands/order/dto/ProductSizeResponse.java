package com.highlands.order.dto;

import com.highlands.order.model.ProductSize;

import java.math.BigDecimal;

public record ProductSizeResponse(
        Long id,
        String sizeName,
        BigDecimal extraPrice
) {
    public static ProductSizeResponse from(ProductSize size) {
        return new ProductSizeResponse(
                size.getId(),
                size.getSizeName(),
                size.getExtraPrice()
        );
    }
}
