package com.highlands.order.service;

import com.highlands.order.dto.CouponRequest;
import com.highlands.order.dto.CouponResponse;
import com.highlands.order.dto.ValidateCouponResponse;
import com.highlands.order.exception.ApiDomainException;
import com.highlands.order.exception.ApiErrorCode;
import com.highlands.order.model.Coupon;
import com.highlands.order.model.CouponType;
import com.highlands.order.repository.CouponRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(CouponResponse::from)
                .toList();
    }

    private Coupon findCoupon(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ApiDomainException(
                        ApiErrorCode.COUPON_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy mã giảm giá ID: " + id
                ));
    }

    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        String code = request.code().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiDomainException(ApiErrorCode.CONFLICT, HttpStatus.CONFLICT,
                    "Mã giảm giá '" + code + "' đã tồn tại");
        }

        Coupon coupon = Coupon.builder()
                .code(code)
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minOrderAmount(request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO)
                .maxDiscountAmount(request.maxDiscountAmount())
                .active(request.active() != null ? request.active() : true)
                .expiryDate(request.expiryDate())
                .build();

        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = findCoupon(id);
        String code = request.code().trim().toUpperCase();

        if (!coupon.getCode().equalsIgnoreCase(code) && couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiDomainException(ApiErrorCode.CONFLICT, HttpStatus.CONFLICT,
                    "Mã giảm giá '" + code + "' đã tồn tại");
        }

        coupon.setCode(code);
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinOrderAmount(request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO);
        coupon.setMaxDiscountAmount(request.maxDiscountAmount());
        if (request.active() != null) {
            coupon.setActive(request.active());
        }
        coupon.setExpiryDate(request.expiryDate());

        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = findCoupon(id);
        couponRepository.delete(coupon);
    }

    public ValidateCouponResponse validateAndCalculateDiscount(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Mã giảm giá không được để trống");
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ApiDomainException(
                        ApiErrorCode.COUPON_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Mã giảm giá '" + code + "' không hợp lệ"
                ));

        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new ApiDomainException(ApiErrorCode.COUPON_INACTIVE, HttpStatus.BAD_REQUEST,
                    "Mã giảm giá '" + code + "' đã bị tạm ngưng");
        }

        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ApiDomainException(ApiErrorCode.COUPON_EXPIRED, HttpStatus.BAD_REQUEST,
                    "Mã giảm giá '" + code + "' đã hết hạn sử dụng");
        }

        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new ApiDomainException(ApiErrorCode.COUPON_MINIMUM_NOT_MET, HttpStatus.BAD_REQUEST,
                    "Đơn hàng tối thiểu để áp dụng mã là " + coupon.getMinOrderAmount() + " VNĐ");
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getDiscountType() == CouponType.FIXED) {
            discount = coupon.getDiscountValue();
        } else if (coupon.getDiscountType() == CouponType.PERCENT) {
            discount = subtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        }

        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        return new ValidateCouponResponse(coupon.getCode(), discount, "Áp dụng mã giảm giá thành công");
    }
}
