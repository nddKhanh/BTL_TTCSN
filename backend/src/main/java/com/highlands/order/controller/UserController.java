package com.highlands.order.controller;

import com.highlands.order.dto.*;
import com.highlands.order.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final AuthService authService;
    public UserController(AuthService authService) { this.authService = authService; }
    @GetMapping public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserDetails user) { return ApiResponse.success(authService.me(user.getUsername())); }
    @PutMapping public ApiResponse<UserResponse> update(@AuthenticationPrincipal UserDetails user, @Valid @RequestBody UpdateProfileRequest request) { return ApiResponse.success("Cập nhật hồ sơ thành công", authService.updateProfile(user.getUsername(), request)); }
}
