package com.highlands.order.service;

import com.highlands.order.dto.CreateOrderRequest;
import com.highlands.order.dto.OrderItemRequest;
import com.highlands.order.config.OrderPricingProperties;
import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.*;
import com.highlands.order.repository.OrderRepository;
import com.highlands.order.repository.ProductRepository;
import com.highlands.order.repository.ToppingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final OrderPricingProperties pricing;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        ToppingRepository toppingRepository, OrderPricingProperties pricing) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
        this.pricing = pricing;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        String orderCode = "COF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Order order = Order.builder()
                .orderCode(orderCode)
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .deliveryAddress(request.deliveryAddress())
                .note(request.note())
                .status(OrderStatus.PENDING)
                .subtotal(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal grandTotal = BigDecimal.ZERO;
        Map<String, BigDecimal> toppingPriceMap = toppingRepository.findAll().stream()
                .collect(Collectors.toMap(Topping::getName, Topping::getPrice));

        for (OrderItemRequest itemReq : request.items()) {
            Product product = productRepository.findByIdAndIsActiveTrue(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm ID " + itemReq.productId() + " không tồn tại"));

            BigDecimal unitPrice = product.getBasePrice();

            // Tính giá phụ thu của Size
            ProductSize selectedSize = selectSize(product, itemReq.sizeName());
            unitPrice = unitPrice.add(selectedSize.getExtraPrice());

            // Tính giá các Toppings
            String toppingsStr = "";
            if (itemReq.toppings() != null && !itemReq.toppings().isEmpty()) {
                toppingsStr = String.join(", ", itemReq.toppings());
                Set<String> uniqueToppings = Set.copyOf(itemReq.toppings());
                if (uniqueToppings.size() != itemReq.toppings().size()) {
                    throw new IllegalArgumentException("Không được chọn topping trùng lặp");
                }
                for (String tName : uniqueToppings) {
                    BigDecimal tPrice = toppingPriceMap.get(tName);
                    if (tPrice == null) {
                        throw new IllegalArgumentException("Topping không hợp lệ: " + tName);
                    }
                    unitPrice = unitPrice.add(tPrice);
                }
            }

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));
            grandTotal = grandTotal.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .sizeName(selectedSize.getSizeName())
                    .toppings(toppingsStr)
                    .quantity(itemReq.quantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            order.getItems().add(orderItem);
        }

        BigDecimal shippingFee = grandTotal.compareTo(pricing.freeShippingThreshold()) >= 0 ? BigDecimal.ZERO : pricing.shippingFee();
        order.setSubtotal(grandTotal);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(grandTotal.add(shippingFee));
        return orderRepository.save(order);
    }

    public Order getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng mã: " + orderCode));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String requestedStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(requestedStatus.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Trạng thái đơn không hợp lệ: " + requestedStatus);
        }
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new IllegalArgumentException("Không thể chuyển đơn từ " + order.getStatus() + " sang " + newStatus);
        }
        order.setStatus(newStatus);
        return orderRepository.save(order);
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
