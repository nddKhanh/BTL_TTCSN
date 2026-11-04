package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.service.SecurityAuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/security-audit")
@PreAuthorize("hasAuthority('REPORT_READ')")
public class AdminSecurityAuditController {
    private final SecurityAuditService audit;

    public AdminSecurityAuditController(SecurityAuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    public ApiResponse<List<SecurityAuditService.AuditView>> search(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.success(audit.search(eventType, result, from, to));
    }
}
