package com.highlands.order.application.order;

import com.highlands.order.application.approval.ApprovalApplicationService;
import com.highlands.order.domain.approval.ApprovalActor;
import com.highlands.order.dto.OrderResponse;
import org.springframework.stereotype.Service;

@Service
public class CancelOrderService {

    private final ApprovalApplicationService approvals;

    public CancelOrderService(ApprovalApplicationService approvals) {
        this.approvals = approvals;
    }

    public OrderResponse withdraw(Long orderId, ApprovalActor actor) {
        if (!actor.authenticated()) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để hủy đơn hàng");
        }
        return approvals.legacyWithdraw(orderId, actor);
    }
}
