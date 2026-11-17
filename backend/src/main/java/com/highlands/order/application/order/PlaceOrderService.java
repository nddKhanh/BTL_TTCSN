package com.highlands.order.application.order;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.domain.approval.ApprovalActor;
import com.highlands.order.domain.fulfillment.FulfillmentStatus;
import com.highlands.order.dto.*;
import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.*;
import com.highlands.order.repository.*;
import com.highlands.order.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlaceOrderService {

    private final OrderRepository orders;
    private final ProductRepository products;
    private final ToppingRepository toppings;
    private final UserRepository users;
    private final CouponService coupons;
    private final OrderPricingService pricing;
    private final ApprovalPort approvalPort;

    public PlaceOrderService(
            OrderRepository orders,
            ProductRepository products,
            ToppingRepository toppings,
            UserRepository users,
            CouponService coupons,
            OrderPricingService pricing,
            ApprovalPort approvalPort
    ) {
        this.orders = orders;
        this.products = products;
        this.toppings = toppings;
        this.users = users;
        this.coupons = coupons;
        this.pricing = pricing;
        this.approvalPort = approvalPort;
    }

    @Transactional
    public OrderResponse place(CreateOrderRequest request, ApprovalActor actor) {
        String orderCode = "COF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = Order.builder()
                .orderCode(orderCode)
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .deliveryAddress(request.deliveryAddress())
                .note(request.note())
                .status(OrderStatus.PENDING)
                .fulfillmentStatus(FulfillmentStatus.NOT_STARTED)
                .subtotal(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        Map<String, BigDecimal> toppingPrices = toppings.findAll().stream()
                .collect(Collectors.toMap(Topping::getName, Topping::getPrice));
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = products.findByIdAndIsActiveTrue(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sản phẩm ID " + itemRequest.productId() + " không tồn tại"));
            var line = pricing.priceLine(product, itemRequest, toppingPrices);
            subtotal = subtotal.add(line.subtotal());
            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .sizeName(line.size().getSizeName())
                    .toppings(line.toppings())
                    .iceLevel(itemRequest.iceLevel())
                    .sugarLevel(itemRequest.sugarLevel())
                    .note(itemRequest.note())
                    .quantity(itemRequest.quantity())
                    .unitPrice(line.unitPrice())
                    .subtotal(line.subtotal())
                    .build());
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            ValidateCouponResponse result = coupons.validateAndCalculateDiscount(request.couponCode(), subtotal);
            order.setCouponCode(result.code());
            discount = result.discountAmount();
        }
        var totals = pricing.calculateTotals(subtotal, discount);
        order.setDiscountAmount(totals.discount());
        order.setSubtotal(totals.subtotal());
        order.setShippingFee(totals.shippingFee());
        order.setTotalAmount(totals.total());

        ApprovalActor submissionActor = actor;
        if (actor.authenticated()) {
            AppUser user = users.findById(actor.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản đặt hàng"));
            order.setUser(user);
        } else {
            submissionActor = ApprovalActor.guestOrder(orderCode);
        }

        order = orders.save(order);
        ApprovalPort.Snapshot approval = approvalPort.submit(
                new ApprovalPort.Submission(order.getId(), orderCode, order.getCustomerName(), order.getTotalAmount()),
                submissionActor,
                "submit:" + orderCode
        );
        return OrderResponse.from(order, approval, approvalPort.availableActionDetails(approval, actor), actor);
    }
}
