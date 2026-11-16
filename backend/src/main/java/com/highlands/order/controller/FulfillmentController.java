package com.highlands.order.controller;

import com.highlands.order.application.approval.WorkflowActorResolver;
import com.highlands.order.application.fulfillment.FulfillmentApplicationService;
import com.highlands.order.domain.fulfillment.FulfillmentAction;
import com.highlands.order.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/fulfillment")
@SecurityRequirement(name = "bearerAuth")
public class FulfillmentController {

    private final FulfillmentApplicationService fulfillment;
    private final WorkflowActorResolver actors;

    public FulfillmentController(FulfillmentApplicationService fulfillment, WorkflowActorResolver actors) {
        this.fulfillment = fulfillment;
        this.actors = actors;
    }

    @PostMapping("/actions/{action}")
    @Operation(summary = "Thực hiện action chuẩn bị và giao hàng")
    public ApiResponse<OrderResponse> execute(
            @PathVariable Long orderId,
            @PathVariable String action,
            @Valid @RequestBody WorkflowActionRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        OrderResponse result = fulfillment.execute(
                orderId,
                FulfillmentAction.fromApiName(action),
                actors.resolve(user),
                request.reason(),
                request.requestId(),
                request.expectedVersion()
        );
        return ApiResponse.success("Đã cập nhật tiến độ đơn hàng", result);
    }

    @GetMapping("/history")
    public ApiResponse<List<WorkflowHistoryResponse>> history(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(fulfillment.history(orderId, actors.resolve(user)));
    }
}
