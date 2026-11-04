package com.highlands.order.controller;

import com.highlands.order.dto.*;
import com.highlands.order.service.AuthService;
import com.highlands.order.service.AccountLifecycleService;
import com.highlands.order.service.RefreshSessionService;
import com.highlands.order.service.MfaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshSessionService refreshSessions;
    private final AccountLifecycleService lifecycle;
    private final MfaService mfa;
    public AuthController(AuthService authService, RefreshSessionService refreshSessions,
                          AccountLifecycleService lifecycle, MfaService mfa) {
        this.authService = authService; this.refreshSessions = refreshSessions; this.lifecycle = lifecycle; this.mfa = mfa;
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) { return ApiResponse.success("Đăng ký thành công", authService.register(request)); }
    @PostMapping("/login")
    public ApiResponse<RefreshSessionService.TokenPair> login(@Valid @RequestBody LoginRequest request,
                                                               HttpServletRequest httpRequest) {
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        String ip = forwarded == null ? httpRequest.getRemoteAddr() : forwarded.split(",")[0].trim();
        return ApiResponse.success("Đăng nhập thành công", authService.loginSession(
                request, ip, httpRequest.getHeader("User-Agent")));
    }
    @PostMapping("/refresh")
    public ApiResponse<RefreshSessionService.TokenPair> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(refreshSessions.rotate(request.refreshToken()));
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody TokenRequest request) {
        lifecycle.verifyEmail(request.token());
        return ApiResponse.success("Xác minh email thành công", null);
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resend(@Valid @RequestBody EmailRequest request) {
        authService.resendVerification(request.email());
        return ApiResponse.success("Nếu tài khoản tồn tại, email xác minh đã được gửi", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
        lifecycle.requestPasswordReset(request.email());
        return ApiResponse.success("Nếu tài khoản tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        lifecycle.resetPassword(request.token(), request.newPassword());
        return ApiResponse.success("Đặt lại mật khẩu thành công", null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshSessions.revokeCurrent(request.refreshToken());
        return ApiResponse.success("Đăng xuất thành công", null);
    }

    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAll(@AuthenticationPrincipal UserDetails user) {
        if (user != null) refreshSessions.revokeAll(user.getUsername());
        return ApiResponse.success("Đã đăng xuất khỏi tất cả thiết bị", null);
    }

    @PostMapping("/mfa/setup")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MfaService.Setup> setupMfa(@AuthenticationPrincipal UserDetails user) {
        return ApiResponse.success(mfa.start(user.getUsername()));
    }

    @PostMapping("/mfa/confirm")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<java.util.List<String>> confirmMfa(@AuthenticationPrincipal UserDetails user,
                                                          @Valid @RequestBody MfaCodeRequest request) {
        return ApiResponse.success("MFA đã được bật; hãy lưu recovery codes ở nơi an toàn",
                mfa.confirm(user.getUsername(), request.code()));
    }

    @PostMapping("/mfa/challenge")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> challengeMfa(@AuthenticationPrincipal UserDetails user,
                                          @Valid @RequestBody MfaCodeRequest request) {
        mfa.challenge(user.getUsername(), request.code());
        return ApiResponse.success("Xác thực MFA thành công", null);
    }

    public record RefreshRequest(@NotBlank @Size(max = 128) String refreshToken) { }
    public record TokenRequest(@NotBlank @Size(max = 128) String token) { }
    public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) { }
    public record ResetPasswordRequest(@NotBlank @Size(max = 128) String token,
                                       @NotBlank @Size(min = 8, max = 72) String newPassword) { }
    public record MfaCodeRequest(@NotBlank @Pattern(regexp = "(?:[0-9]{6}|[A-Fa-f0-9]{16})") String code) { }
}
