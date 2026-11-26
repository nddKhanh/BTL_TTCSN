package com.highlands.order.dto;

import com.highlands.order.model.Topping;

import java.math.BigDecimal;

public record ToppingResponse(
        Long id,
        String name,
        BigDecimal price
) {
    public static ToppingResponse from(Topping topping) {
        return new ToppingResponse(
                topping.getId(),
                topping.getName(),
                topping.getPrice()
        );
    }
}
