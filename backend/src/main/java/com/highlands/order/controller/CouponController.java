package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.dto.ValidateCouponRequest;
import com.highlands.order.dto.ValidateCouponResponse;
import com.highlands.order.service.CouponService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "Coupons", description = "Kiểm tra mã giảm giá cho đơn hàng")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/validate")
    @Operation(summary = "Kiểm tra mã giảm giá", description = "Tính số tiền giảm theo subtotal và điều kiện coupon.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Coupon hợp lệ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Coupon không hoạt động, hết hạn hoặc chưa đạt đơn tối thiểu", content = @Content(examples = @ExampleObject(value = "{\"success\":false,\"errorCode\":\"COUPON_EXPIRED\"}")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy coupon", content = @Content(examples = @ExampleObject(value = "{\"success\":false,\"errorCode\":\"COUPON_NOT_FOUND\"}")))
    public ApiResponse<ValidateCouponResponse> validateCoupon(@Valid @RequestBody ValidateCouponRequest request) {
        ValidateCouponResponse result = couponService.validateAndCalculateDiscount(request.code(), request.subtotal());
        return ApiResponse.success("Mã hợp lệ", result);
    }
}
