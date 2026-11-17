package com.highlands.order.application.order;

import com.highlands.order.config.OrderPricingProperties;
import com.highlands.order.dto.OrderItemRequest;
import com.highlands.order.model.Product;
import com.highlands.order.model.ProductSize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderPricingService {

    public record PricedLine(
            ProductSize size,
            String toppings,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
    }

    public record Totals(
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal shippingFee,
            BigDecimal total
    ) {
    }

    private final OrderPricingProperties pricing;

    public OrderPricingService(OrderPricingProperties pricing) {
        this.pricing = pricing;
    }

    public PricedLine priceLine(
            Product product,
            OrderItemRequest request,
            Map<String, BigDecimal> toppingPrices
    ) {
        ProductSize size = selectSize(product, request.sizeName());
        BigDecimal unitPrice = product.getBasePrice().add(size.getExtraPrice());
        List<String> requestedToppings = request.toppings() == null ? List.of() : request.toppings();
        Set<String> uniqueToppings = Set.copyOf(requestedToppings);
        if (uniqueToppings.size() != requestedToppings.size()) {
            throw new IllegalArgumentException("Không được chọn topping trùng lặp");
        }
        for (String topping : uniqueToppings) {
            BigDecimal toppingPrice = toppingPrices.get(topping);
            if (toppingPrice == null) {
                throw new IllegalArgumentException("Topping không hợp lệ: " + topping);
            }
            unitPrice = unitPrice.add(toppingPrice);
        }
        return new PricedLine(
                size,
                String.join(", ", requestedToppings),
                unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(request.quantity()))
        );
    }

    public Totals calculateTotals(BigDecimal subtotal, BigDecimal discount) {
        BigDecimal safeDiscount = discount == null ? BigDecimal.ZERO : discount;
        BigDecimal netSubtotal = subtotal.subtract(safeDiscount).max(BigDecimal.ZERO);
        BigDecimal shippingFee = subtotal.compareTo(pricing.freeShippingThreshold()) >= 0
                ? BigDecimal.ZERO
                : pricing.shippingFee();
        return new Totals(subtotal, safeDiscount, shippingFee, netSubtotal.add(shippingFee));
    }

    private ProductSize selectSize(Product product, String requestedSize) {
        if (product.getSizes().isEmpty()) {
            throw new IllegalArgumentException("Sản phẩm chưa được cấu hình size: " + product.getName());
        }
        if (requestedSize == null || requestedSize.isBlank()) {
            return product.getSizes().get(0);
        }
        return product.getSizes().stream()
                .filter(size -> size.getSizeName().equalsIgnoreCase(requestedSize.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Size không hợp lệ cho sản phẩm " + product.getName()));
    }
}
