package com.highlands.order.dto;

import com.highlands.order.model.Category;

public record CategoryResponse(
        Long id,
        String code,
        String name,
        String imageUrl
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getImageUrl()
        );
    }
}
