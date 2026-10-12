package com.highlands.order.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.highlands.order.model.AppUser;
import com.highlands.order.model.Order;
import com.highlands.order.model.OrderItem;
import com.highlands.order.model.Product;
import com.highlands.order.model.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OrderResponseTest {

    @Test
    void from_doesNotExposeUserOrPassword() throws Exception {
        AppUser user = AppUser.builder()
                .id(7L)
                .email("customer@example.com")
                .passwordHash("secret-hash")
                .role(Role.CUSTOMER)
                .build();
        Product product = Product.builder()
                .id(12L)
                .name("Cà phê sữa")
                .build();
        OrderItem item = OrderItem.builder()
                .id(20L)
                .product(product)
                .productName(product.getName())
                .quantity(1)
                .build();
        Order order = Order.builder()
                .id(30L)
                .orderCode("COF-TEST")
                .user(user)
                .items(List.of(item))
                .build();

        OrderResponse response = OrderResponse.from(order);
        String json = new ObjectMapper().writeValueAsString(response);

        assertEquals(12L, response.items().get(0).productId());
        assertFalse(json.contains("customer@example.com"));
        assertFalse(json.contains("secret-hash"));
        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("userId"));
    }
}
