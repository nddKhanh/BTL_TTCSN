package com.highlands.order.dto;

import com.highlands.order.model.Product;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        CategoryResponse category,
        String name,
        String description,
        BigDecimal basePrice,
        String imageUrl,
        Boolean isActive,
        List<ProductSizeResponse> sizes
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                CategoryResponse.from(product.getCategory()),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getImageUrl(),
                product.getIsActive(),
                product.getSizes().stream()
                        .map(ProductSizeResponse::from)
                        .toList()
        );
    }
}
