package com.highlands.order.service;

import com.highlands.order.dto.CreateOrderRequest;
import com.highlands.order.dto.OrderItemRequest;
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
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        ToppingRepository toppingRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        String orderCode = "HL" + (System.currentTimeMillis() % 10000000) + (10 + new Random().nextInt(90));
        
        Order order = Order.builder()
                .orderCode(orderCode)
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .deliveryAddress(request.deliveryAddress())
                .note(request.note())
                .status("PENDING")
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
            if (itemReq.sizeName() != null) {
                for (ProductSize ps : product.getSizes()) {
                    if (ps.getSizeName().equalsIgnoreCase(itemReq.sizeName())) {
                        unitPrice = unitPrice.add(ps.getExtraPrice());
                        break;
                    }
                }
            }

            // Tính giá các Toppings
            String toppingsStr = "";
            if (itemReq.toppings() != null && !itemReq.toppings().isEmpty()) {
                toppingsStr = String.join(", ", itemReq.toppings());
                for (String tName : itemReq.toppings()) {
                    BigDecimal tPrice = toppingPriceMap.getOrDefault(tName, BigDecimal.ZERO);
                    unitPrice = unitPrice.add(tPrice);
                }
            }

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));
            grandTotal = grandTotal.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .sizeName(itemReq.sizeName() != null ? itemReq.sizeName() : "S")
                    .toppings(toppingsStr)
                    .quantity(itemReq.quantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            order.getItems().add(orderItem);
        }

        order.setTotalAmount(grandTotal);
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
    public Order updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));
        order.setStatus(newStatus.toUpperCase());
        return orderRepository.save(order);
    }
}
