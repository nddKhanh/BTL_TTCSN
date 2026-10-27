package com.highlands.order.service;

import com.highlands.order.dto.*;
import com.highlands.order.model.*;
import com.highlands.order.repository.UserRepository;
import com.highlands.order.security.JwtService;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) { this.users = users; this.encoder = encoder; this.jwt = jwt; }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new IllegalArgumentException("Email đã được sử dụng");
        AppUser user = users.save(AppUser.builder().fullName(request.fullName().trim()).email(email).phone(request.phone()).passwordHash(encoder.encode(request.password())).role(Role.CUSTOMER).build());
        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(this::invalidCredentials);
        if (!encoder.matches(request.password(), user.getPasswordHash())) throw invalidCredentials();
        return response(user);
    }

    public UserResponse me(String email) { return UserResponse.from(findByEmail(email)); }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        AppUser user = findByEmail(email); user.setFullName(request.fullName().trim()); user.setPhone(request.phone());
        return UserResponse.from(user);
    }

    private AppUser findByEmail(String email) { return users.findByEmailIgnoreCase(email).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng")); }
    private AuthResponse response(AppUser user) { return new AuthResponse(jwt.createToken(user), UserResponse.from(user)); }
    private BadCredentialsException invalidCredentials() { return new BadCredentialsException("Email hoặc mật khẩu không đúng"); }
}
