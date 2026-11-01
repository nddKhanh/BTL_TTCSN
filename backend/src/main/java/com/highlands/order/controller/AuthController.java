package com.highlands.order.controller;

import com.highlands.order.dto.*;
import com.highlands.order.service.AuthService;
import com.highlands.order.service.RefreshSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshSessionService refreshSessions;
    public AuthController(AuthService authService, RefreshSessionService refreshSessions) {
        this.authService = authService; this.refreshSessions = refreshSessions;
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

    public record RefreshRequest(String refreshToken) { }
}
