package com.highlands.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record WorkflowActionRequest(
        @NotBlank(message = "Request ID không được để trống")
        @Size(max = 100, message = "Request ID không được vượt quá 100 ký tự")
        String requestId,

        @NotNull(message = "Version không được để trống")
        @PositiveOrZero(message = "Version không hợp lệ")
        Long expectedVersion,

        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
        String reason
) {
}
