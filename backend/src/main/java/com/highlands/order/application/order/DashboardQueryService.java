package com.highlands.order.application.order;

import com.highlands.order.application.approval.ApprovalPort;
import com.highlands.order.domain.approval.ApprovalActor;
import com.highlands.order.dto.DashboardStatsResponse;
import com.highlands.order.dto.OrderResponse;
import com.highlands.order.model.Order;
import com.highlands.order.model.OrderStatus;
import com.highlands.order.repository.OrderRepository;
import com.highlands.order.repository.ProductRepository;
import com.highlands.order.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardQueryService {

    private final OrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;
    private final ApprovalPort approvalPort;

    public DashboardQueryService(
            OrderRepository orders,
            ProductRepository products,
            UserRepository users,
            ApprovalPort approvalPort
    ) {
        this.orders = orders;
        this.products = products;
        this.users = users;
        this.approvalPort = approvalPort;
    }

    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    public DashboardStatsResponse getStats(LocalDate from, LocalDate to, ApprovalActor actor) {
        if (from == null && to == null) {
            return allTime(actor);
        }
        LocalDate start = from == null ? to : from;
        LocalDate end = to == null ? from : to;
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.plusDays(1).atStartOfDay();
        long completed = orders.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                OrderStatus.COMPLETED,
                startTime,
                endTime
        );
        return new DashboardStatsResponse(
                orders.sumTotalAmountByStatusAndCreatedAtBetween(OrderStatus.COMPLETED, startTime, endTime),
                completed,
                orders.countByStatus(OrderStatus.PENDING),
                completed,
                orders.countByStatus(OrderStatus.CANCELLED),
                products.countByIsActiveTrue(),
                users.count(),
                recentOrders(actor)
        );
    }

    private DashboardStatsResponse allTime(ApprovalActor actor) {
        List<Order> completedOrders = orders.findByStatus(OrderStatus.COMPLETED);
        BigDecimal revenue = completedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardStatsResponse(
                revenue,
                orders.count(),
                orders.countByStatus(OrderStatus.PENDING),
                completedOrders.size(),
                orders.countByStatus(OrderStatus.CANCELLED),
                products.countByIsActiveTrue(),
                users.count(),
                recentOrders(actor)
        );
    }

    private List<OrderResponse> recentOrders(ApprovalActor actor) {
        List<Order> recent = orders.findTop5ByOrderByCreatedAtDesc();
        Map<Long, ApprovalPort.Snapshot> approvals = approvalPort.findByOrderIds(
                recent.stream().map(Order::getId).toList()
        );
        return recent.stream()
                .map(order -> {
                    ApprovalPort.Snapshot approval = approvals.get(order.getId());
                    return OrderResponse.from(
                            order,
                            approval,
                            approval == null ? List.of() : approvalPort.availableActionDetails(approval, actor),
                            actor
                    );
                })
                .toList();
    }
}
