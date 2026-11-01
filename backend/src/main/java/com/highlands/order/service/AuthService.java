package com.highlands.order.service;

import com.highlands.order.dto.*;
import com.highlands.order.config.AccountSecurityProperties;
import com.highlands.order.model.*;
import com.highlands.order.repository.UserRepository;
import com.highlands.order.security.JwtService;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
    private final RefreshSessionService refreshSessions;
    private final AccountSecurityProperties securityProperties;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                       RefreshSessionService refreshSessions, AccountSecurityProperties securityProperties) {
        this.users = users; this.encoder = encoder; this.jwt = jwt; this.refreshSessions = refreshSessions;
        this.securityProperties = securityProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new IllegalArgumentException("Email đã được sử dụng");
        AppUser user = users.save(AppUser.builder().fullName(request.fullName().trim()).email(email).phone(request.phone()).passwordHash(encoder.encode(request.password())).role(Role.CUSTOMER).build());
        return response(user);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse login(LoginRequest request) {
        AppUser user = authenticate(request);
        return response(user);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public RefreshSessionService.TokenPair loginSession(LoginRequest request) {
        return refreshSessions.issue(authenticate(request));
    }

    private AppUser authenticate(LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email().trim()).orElseThrow(this::invalidCredentials);
        LocalDateTime now = LocalDateTime.now();
        if (user.getAccountStatus() == AccountStatus.DISABLED
                || user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            throw invalidCredentials();
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw invalidCredentials();
        }
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            int failures = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failures);
            if (failures >= securityProperties.maxFailures()) {
                user.setAccountStatus(AccountStatus.LOCKED);
                user.setLockedUntil(now.plusMinutes(securityProperties.lockMinutes()));
            }
            throw invalidCredentials();
        }
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setAccountStatus(AccountStatus.ACTIVE);
        return user;
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
