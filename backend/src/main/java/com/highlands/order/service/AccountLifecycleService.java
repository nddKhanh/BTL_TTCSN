package com.highlands.order.service;

import com.highlands.order.mail.MailGateway;
import com.highlands.order.model.AccountStatus;
import com.highlands.order.model.AppUser;
import com.highlands.order.model.AuthToken;
import com.highlands.order.repository.AuthTokenRepository;
import com.highlands.order.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@Transactional
public class AccountLifecycleService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthTokenRepository tokens;
    private final UserRepository users;
    private final MailGateway mail;

    public AccountLifecycleService(AuthTokenRepository tokens, UserRepository users, MailGateway mail) {
        this.tokens = tokens; this.users = users; this.mail = mail;
    }

    public void issueVerification(AppUser user) {
        tokens.deleteByUserIdAndPurpose(user.getId(), AuthToken.TokenPurpose.EMAIL_VERIFICATION);
        String raw = randomToken();
        tokens.save(AuthToken.builder().user(user).purpose(AuthToken.TokenPurpose.EMAIL_VERIFICATION)
                .tokenHash(RefreshSessionService.hash(raw)).expiresAt(LocalDateTime.now().plusHours(24)).build());
        mail.send(user.getEmail(), "Xác minh tài khoản Mộc Nhiên", "Mã xác minh: " + raw);
    }

    public void verifyEmail(String rawToken) {
        AuthToken token = validToken(rawToken, AuthToken.TokenPurpose.EMAIL_VERIFICATION);
        token.setUsedAt(LocalDateTime.now());
        AppUser user = token.getUser();
        user.setEmailVerified(true);
        user.setAccountStatus(AccountStatus.ACTIVE);
        users.save(user);
    }

    private AuthToken validToken(String rawToken, AuthToken.TokenPurpose purpose) {
        AuthToken token = tokens.findByTokenHashAndPurpose(RefreshSessionService.hash(rawToken), purpose)
                .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ"));
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token đã hết hạn hoặc được sử dụng");
        }
        return token;
    }

    private static String randomToken() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
