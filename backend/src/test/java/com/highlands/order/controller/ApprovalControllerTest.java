package com.highlands.order.controller;

import com.highlands.order.application.approval.ApprovalApplicationService;
import com.highlands.order.application.approval.WorkflowActorResolver;
import com.highlands.order.domain.approval.*;
import com.highlands.order.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApprovalControllerTest {

    @Test
    void actionEndpointReturnsForbiddenConflictAndValidationContracts() throws Exception {
        ApprovalApplicationService approvals = mock(ApprovalApplicationService.class);
        WorkflowActorResolver actors = mock(WorkflowActorResolver.class);
        ApprovalActor actor = new ApprovalActor("user:2", 2L, true, Set.of("ORDER_READ"));
        when(actors.resolve(nullable(UserDetails.class))).thenReturn(actor);
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new ApprovalController(approvals, actors))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        doThrow(new AccessDeniedException("denied"))
                .when(approvals).execute(eq(1L), eq("approve"), eq(actor),
                        isNull(), eq("request-1"), eq(0L));
        mvc.perform(post("/api/v1/orders/1/approval/actions/approve")
                        .contentType("application/json")
                        .content("""
                                {"requestId":"request-1","expectedVersion":0}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        doThrow(new ApiDomainException(
                ApiErrorCode.WORKFLOW_CONFLICT,
                HttpStatus.CONFLICT,
                "Version cũ"
        )).when(approvals).execute(eq(1L), eq("approve"), eq(actor),
                isNull(), eq("request-2"), eq(0L));
        mvc.perform(post("/api/v1/orders/1/approval/actions/approve")
                        .contentType("application/json")
                        .content("""
                                {"requestId":"request-2","expectedVersion":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WORKFLOW_CONFLICT"));

        mvc.perform(post("/api/v1/orders/1/approval/actions/approve")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        doThrow(new ApiDomainException(
                ApiErrorCode.WORKFLOW_CONFLICT,
                HttpStatus.CONFLICT,
                "Đang kiểm chứng action động"
        )).when(approvals).execute(eq(1L), eq("start_review"), eq(actor),
                isNull(), eq("request-3"), eq(0L));
        mvc.perform(post("/api/v1/orders/1/approval/actions/start_review")
                        .contentType("application/json")
                        .content("""
                                {"requestId":"request-3","expectedVersion":0}
                                """))
                .andExpect(status().isConflict());
    }
}
