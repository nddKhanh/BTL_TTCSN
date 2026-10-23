package com.highlands.order.controller;

import com.highlands.order.dto.*;
import com.highlands.order.service.AuthService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Đăng ký và đăng nhập bằng JWT")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đăng ký tài khoản", description = "Tạo tài khoản customer và trả về JWT.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Đăng ký thành công", content = @Content(schema = @Schema(implementation = com.highlands.order.dto.ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email đã được sử dụng", content = @Content(examples = @ExampleObject(value = "{\"success\":false,\"errorCode\":\"EMAIL_ALREADY_REGISTERED\"}")))
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) { return ApiResponse.success("Đăng ký thành công", authService.register(request)); }
    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Xác thực email/mật khẩu và trả về JWT Bearer token.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đăng nhập thành công", content = @Content(schema = @Schema(implementation = com.highlands.order.dto.ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Thông tin xác thực không đúng", content = @Content(examples = @ExampleObject(value = "{\"success\":false,\"errorCode\":\"AUTHENTICATION_FAILED\"}")))
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) { return ApiResponse.success("Đăng nhập thành công", authService.login(request)); }
}
