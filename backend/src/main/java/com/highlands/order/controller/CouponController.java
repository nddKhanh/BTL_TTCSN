package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.dto.ValidateCouponRequest;
import com.highlands.order.dto.ValidateCouponResponse;
import com.highlands.order.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/validate")
    public ApiResponse<ValidateCouponResponse> validateCoupon(@Valid @RequestBody ValidateCouponRequest request) {
        ValidateCouponResponse result = couponService.validateAndCalculateDiscount(request.code(), request.subtotal());
        return ApiResponse.success("Mã hợp lệ", result);
    }
}
