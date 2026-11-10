package com.highlands.order.controller;

import com.highlands.order.application.approval.WorkflowActorResolver;
import com.highlands.order.application.order.DashboardQueryService;
import com.highlands.order.dto.ApiResponse;
import com.highlands.order.dto.DashboardStatsResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final DashboardQueryService dashboard;
    private final WorkflowActorResolver actors;

    public AdminDashboardController(DashboardQueryService dashboard, WorkflowActorResolver actors) {
        this.dashboard = dashboard;
        this.actors = actors;
    }

    @GetMapping
    public ApiResponse<DashboardStatsResponse> getDashboardStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ApiResponse.success(dashboard.getStats(from, to, actors.resolve(user)));
    }
}
