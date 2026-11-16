package com.highlands.order.controller;

import com.highlands.order.application.approval.ApprovalApplicationService;
import com.highlands.order.application.approval.WorkflowActorResolver;
import com.highlands.order.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/approval")
@SecurityRequirement(name = "bearerAuth")
public class ApprovalController {

    private final ApprovalApplicationService approvals;
    private final WorkflowActorResolver actors;

    public ApprovalController(ApprovalApplicationService approvals, WorkflowActorResolver actors) {
        this.approvals = approvals;
        this.actors = actors;
    }

    @GetMapping
    @Operation(summary = "Đọc trạng thái và action xét duyệt hiện có")
    public ApiResponse<ApprovalResponse> get(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(approvals.get(orderId, actors.resolve(user)));
    }

    @GetMapping("/actions")
    public ApiResponse<List<String>> actions(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(approvals.get(orderId, actors.resolve(user)).availableActions());
    }

    @PostMapping("/actions/{action}")
    @Operation(summary = "Thực hiện action xét duyệt có idempotency và optimistic locking")
    public ApiResponse<OrderResponse> execute(
            @PathVariable Long orderId,
            @PathVariable String action,
            @Valid @RequestBody WorkflowActionRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        OrderResponse result = approvals.execute(
                orderId,
                action,
                actors.resolve(user),
                request.reason(),
                request.requestId(),
                request.expectedVersion()
        );
        return ApiResponse.success("Đã cập nhật quyết định xét duyệt", result);
    }

    @GetMapping("/history")
    public ApiResponse<List<WorkflowHistoryResponse>> history(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(approvals.history(orderId, actors.resolve(user)));
    }
}
