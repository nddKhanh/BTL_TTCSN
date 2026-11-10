package com.highlands.order.application.order;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.domain.approval.ApprovalActor;
import com.highlands.order.dto.OrderResponse;
import com.highlands.order.exception.ApiDomainException;
import com.highlands.order.exception.ApiErrorCode;
import com.highlands.order.exception.ResourceNotFoundException;
import com.highlands.order.model.Order;
import com.highlands.order.model.OrderStatus;
import com.highlands.order.repository.OrderRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orders;
    private final ApprovalPort approvalPort;

    public OrderQueryService(OrderRepository orders, ApprovalPort approvalPort) {
        this.orders = orders;
        this.approvalPort = approvalPort;
    }

    public OrderResponse getByCode(String orderCode, String phone, ApprovalActor actor) {
        Order order = orders.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng mã: " + orderCode));
        boolean owner = actor.authenticated()
                && order.getUser() != null
                && actor.userId().equals(order.getUser().getId());
        boolean phoneProof = phone != null
                && !phone.isBlank()
                && order.getCustomerPhone().equals(phone.trim());
        if (!owner && !phoneProof) {
            throw new ApiDomainException(
                    ApiErrorCode.ORDER_LOOKUP_PROOF_REQUIRED,
                    HttpStatus.FORBIDDEN,
                    "Cần cung cấp đúng số điện thoại đặt hàng để tra cứu"
            );
        }
        ApprovalPort.Snapshot approval = approvalPort.findByOrderIds(List.of(order.getId())).get(order.getId());
        return toResponse(order, approval, actor);
    }

    public List<OrderResponse> getMyOrders(ApprovalActor actor) {
        requireAuthenticated(actor);
        return mapOrders(orders.findByUser_IdOrderByCreatedAtDesc(actor.userId()), actor);
    }

    @PreAuthorize("hasAuthority('ORDER_READ')")
    public List<OrderResponse> getAllOrders(
            String requestedStatus,
            String keyword,
            LocalDate createdFrom,
            LocalDate createdTo,
            ApprovalActor actor
    ) {
        QueryFilters filters = filters(requestedStatus, keyword, createdFrom, createdTo);
        return mapOrders(orders.searchForAdmin(
                filters.status(),
                filters.keyword(),
                filters.createdFrom(),
                filters.createdUntil()
        ), actor);
    }

    @PreAuthorize("hasAuthority('ORDER_READ')")
    public Page<OrderResponse> getAllOrdersPage(
            String requestedStatus,
            String keyword,
            LocalDate createdFrom,
            LocalDate createdTo,
            int page,
            int size,
            ApprovalActor actor
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Phân trang không hợp lệ");
        }
        QueryFilters filters = filters(requestedStatus, keyword, createdFrom, createdTo);
        Page<Order> result = orders.searchPageForAdmin(
                filters.status(),
                filters.keyword(),
                filters.createdFrom(),
                filters.createdUntil(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
        return new PageImpl<>(mapOrders(result.getContent(), actor), result.getPageable(), result.getTotalElements());
    }

    public Order getEntity(Long orderId) {
        return orders.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));
    }

    private List<OrderResponse> mapOrders(List<Order> source, ApprovalActor actor) {
        Map<Long, ApprovalPort.Snapshot> approvals = approvalPort.findByOrderIds(
                source.stream().map(Order::getId).toList()
        );
        return source.stream()
                .map(order -> toResponse(order, approvals.get(order.getId()), actor))
                .toList();
    }

    private OrderResponse toResponse(
            Order order,
            ApprovalPort.Snapshot approval,
            ApprovalActor actor
    ) {
        return OrderResponse.from(
                order,
                approval,
                approval == null ? List.of() : approvalPort.availableActionDetails(approval, actor),
                actor
        );
    }

    private QueryFilters filters(
            String requestedStatus,
            String keyword,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        return new QueryFilters(
                parseOptionalStatus(requestedStatus),
                keyword == null || keyword.isBlank() ? null : keyword.trim(),
                createdFrom == null ? null : createdFrom.atStartOfDay(),
                createdTo == null ? null : createdTo.plusDays(1).atStartOfDay()
        );
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

    private void requireAuthenticated(ApprovalActor actor) {
        if (!actor.authenticated()) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để xem lịch sử đơn hàng");
        }
    }

    private record QueryFilters(
            OrderStatus status,
            String keyword,
            LocalDateTime createdFrom,
            LocalDateTime createdUntil
    ) {
    }
}
