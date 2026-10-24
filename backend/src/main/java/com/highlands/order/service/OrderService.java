package com.highlands.order.service;

import com.highlands.order.dto.CreateOrderRequest;
import com.highlands.order.dto.DashboardStatsResponse;
import com.highlands.order.dto.OrderItemRequest;
import com.highlands.order.dto.OrderResponse;
import com.highlands.order.dto.ValidateCouponResponse;
import com.highlands.order.config.OrderPricingProperties;
import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.*;
import com.highlands.order.repository.OrderRepository;
import com.highlands.order.repository.ProductRepository;
import com.highlands.order.repository.ToppingRepository;
import com.highlands.order.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final UserRepository userRepository;
    private final CouponService couponService;
    private final OrderPricingProperties pricing;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        ToppingRepository toppingRepository,
                        UserRepository userRepository,
                        CouponService couponService,
                        OrderPricingProperties pricing) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
        this.userRepository = userRepository;
        this.couponService = couponService;
        this.pricing = pricing;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String authenticatedEmail) {
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
                    .iceLevel(itemReq.iceLevel())
                    .sugarLevel(itemReq.sugarLevel())
                    .note(itemReq.note())
                    .quantity(itemReq.quantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            order.getItems().add(orderItem);
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            ValidateCouponResponse couponRes = couponService.validateAndCalculateDiscount(request.couponCode(), grandTotal);
            discountAmount = couponRes.discountAmount();
            order.setCouponCode(couponRes.code());
            order.setDiscountAmount(discountAmount);
        }

        BigDecimal netSubtotal = grandTotal.subtract(discountAmount);
        if (netSubtotal.compareTo(BigDecimal.ZERO) < 0) {
            netSubtotal = BigDecimal.ZERO;
        }

        BigDecimal shippingFee = grandTotal.compareTo(pricing.freeShippingThreshold()) >= 0 ? BigDecimal.ZERO : pricing.shippingFee();
        order.setSubtotal(grandTotal);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(netSubtotal.add(shippingFee));

        if (authenticatedEmail != null && !authenticatedEmail.isBlank()) {
            AppUser authenticatedUser = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản đặt hàng"));
            order.setUser(authenticatedUser);
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    public OrderResponse getOrderByCode(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng mã: " + orderCode));
        return OrderResponse.from(order);
    }

    public List<OrderResponse> getMyOrders(String email) {
        return orderRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(email).stream()
                .map(OrderResponse::from)
                .toList();
    }

    public List<OrderResponse> getAllOrders(
            String requestedStatus,
            String keyword,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {
        OrderStatus status = parseOptionalStatus(requestedStatus);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        LocalDateTime fromDateTime = createdFrom == null ? null : createdFrom.atStartOfDay();
        LocalDateTime untilDateTime = createdTo == null ? null : createdTo.plusDays(1).atStartOfDay();

        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
        }

        return orderRepository.searchForAdmin(status, normalizedKeyword, fromDateTime, untilDateTime).stream()
                .map(OrderResponse::from)
                .toList();
    }

    public DashboardStatsResponse getDashboardStats() {
        List<Order> completedOrders = orderRepository.findByStatus(OrderStatus.COMPLETED);
        BigDecimal totalRevenue = completedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long completedCount = completedOrders.size();
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long totalProducts = productRepository.countByIsActiveTrue();
        long totalUsers = userRepository.count();
        List<OrderResponse> recentOrders = orderRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(OrderResponse::from)
                .toList();

        return new DashboardStatsResponse(
                totalRevenue,
                totalOrders,
                pendingOrders,
                completedCount,
                cancelledOrders,
                totalProducts,
                totalUsers,
                recentOrders
        );
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String requestedStatus) {
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
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelMyOrder(Long orderId, String email) {
        Order order = orderRepository.findByIdAndUserEmailIgnoreCase(orderId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng của bạn"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ có thể hủy đơn đang chờ xác nhận");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(orderRepository.save(order));
    }

    private OrderStatus parseOptionalStatus(String requestedStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(requestedStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Trạng thái đơn không hợp lệ: " + requestedStatus);
        }
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
