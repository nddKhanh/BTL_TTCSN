package com.highlands.order.controller;

import com.highlands.order.dto.ApiResponse;
import com.highlands.order.dto.CouponRequest;
import com.highlands.order.model.Coupon;
import com.highlands.order.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/coupons")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    public ApiResponse<List<Coupon>> getAllCoupons() {
        return ApiResponse.success(couponService.getAllCoupons());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Coupon> createCoupon(@Valid @RequestBody CouponRequest request) {
        Coupon created = couponService.createCoupon(request);
        return ApiResponse.success("Tạo mã giảm giá thành công", created);
    }

    @PutMapping("/{id}")
    public ApiResponse<Coupon> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        Coupon updated = couponService.updateCoupon(id, request);
        return ApiResponse.success("Cập nhật mã giảm giá thành công", updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
    }
}
