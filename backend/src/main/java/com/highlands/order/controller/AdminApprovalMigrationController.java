package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.dto.ApprovalMigrationReport;
import com.highlands.order.infrastructure.approval.DxosApprovalMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/approval-migration")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "app.approval", name = "provider", havingValue = "dxos")
public class AdminApprovalMigrationController {

    private final DxosApprovalMigrationService migration;

    public AdminApprovalMigrationController(DxosApprovalMigrationService migration) {
        this.migration = migration;
    }

    @GetMapping("/preflight")
    @Operation(summary = "Kiểm tra khả năng chuyển approval Local sang DX-OS")
    public ApiResponse<ApprovalMigrationReport> preflight() {
        return ApiResponse.success(migration.preflight());
    }

    @PostMapping
    @Operation(summary = "Chuyển approval Local sang DX-OS theo workflow metadata hiện tại")
    public ApiResponse<ApprovalMigrationReport> migrate() {
        return ApiResponse.success("Đã hoàn tất lượt chuyển đổi approval", migration.migrate());
    }
}
