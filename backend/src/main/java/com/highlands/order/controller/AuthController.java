package com.highlands.order.controller;

import com.highlands.order.dto.*;
import com.highlands.order.service.AuthService;
import com.highlands.order.service.AccountLifecycleService;
import com.highlands.order.service.RefreshSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshSessionService refreshSessions;
    private final AccountLifecycleService lifecycle;
    public AuthController(AuthService authService, RefreshSessionService refreshSessions,
                          AccountLifecycleService lifecycle) {
        this.authService = authService; this.refreshSessions = refreshSessions; this.lifecycle = lifecycle;
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) { return ApiResponse.success("Đăng ký thành công", authService.register(request)); }
    @PostMapping("/login")
    public ApiResponse<RefreshSessionService.TokenPair> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Đăng nhập thành công", authService.loginSession(request));
    }
    @PostMapping("/refresh")
    public ApiResponse<RefreshSessionService.TokenPair> refresh(@RequestBody RefreshRequest request) {
        return ApiResponse.success(refreshSessions.rotate(request.refreshToken()));
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestBody TokenRequest request) {
        lifecycle.verifyEmail(request.token());
        return ApiResponse.success("Xác minh email thành công", null);
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resend(@RequestBody EmailRequest request) {
        authService.resendVerification(request.email());
        return ApiResponse.success("Nếu tài khoản tồn tại, email xác minh đã được gửi", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@RequestBody EmailRequest request) {
        lifecycle.requestPasswordReset(request.email());
        return ApiResponse.success("Nếu tài khoản tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        lifecycle.resetPassword(request.token(), request.newPassword());
        return ApiResponse.success("Đặt lại mật khẩu thành công", null);
    }

    public record RefreshRequest(String refreshToken) { }
    public record TokenRequest(String token) { }
    public record EmailRequest(String email) { }
    public record ResetPasswordRequest(String token, String newPassword) { }
}
