package com.highlands.order.service;

import com.highlands.order.dto.UserResponse;
import com.highlands.order.model.AppUser;
import com.highlands.order.model.AccountStatus;
import com.highlands.order.model.RefreshSession;
import com.highlands.order.repository.RefreshSessionRepository;
import com.highlands.order.repository.UserRepository;
import com.highlands.order.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class RefreshSessionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshSessionRepository sessions;
    private final JwtService jwt;
    private final UserRepository users;

    public RefreshSessionService(RefreshSessionRepository sessions, JwtService jwt, UserRepository users) {
        this.sessions = sessions; this.jwt = jwt; this.users = users;
    }

    public TokenPair issue(AppUser user) {
        return create(user, UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public TokenPair rotate(String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        RefreshSession current = sessions.findByTokenHash(hash(rawToken))
                .orElseThrow(this::invalidToken);
        if (!current.activeAt(now)) {
            sessions.findAllByFamilyIdAndRevokedAtIsNull(current.getFamilyId())
                    .forEach(session -> session.setRevokedAt(now));
            throw invalidToken();
        }
        AppUser user = current.getUser();
        if (user.getAccountStatus() != AccountStatus.ACTIVE
                || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))
                || (user.getPasswordChangedAt() != null && (current.getCreatedAt() == null
                    || !current.getCreatedAt().isAfter(user.getPasswordChangedAt())))) {
            sessions.findAllByUserIdAndRevokedAtIsNull(user.getId())
                    .forEach(session -> session.setRevokedAt(now));
            throw invalidToken();
        }
        TokenPair next = create(current.getUser(), current.getFamilyId());
        current.setRevokedAt(now);
        current.setReplacedByHash(hash(next.refreshToken()));
        return next;
    }

    public void revokeCurrent(String rawToken) {
        sessions.findByTokenHash(hash(rawToken)).ifPresent(session -> session.setRevokedAt(LocalDateTime.now()));
    }

    public void revokeAll(String email) {
        users.findByEmailIgnoreCase(email).ifPresent(user ->
                sessions.findAllByUserIdAndRevokedAtIsNull(user.getId())
                        .forEach(session -> session.setRevokedAt(LocalDateTime.now())));
    }

    private TokenPair create(AppUser user, String familyId) {
        String raw = randomToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(14);
        sessions.save(RefreshSession.builder().user(user).familyId(familyId)
                .tokenHash(hash(raw)).expiresAt(expiresAt).build());
        return new TokenPair(jwt.createToken(user), raw, expiresAt, UserResponse.from(user));
    }

    static String hash(String value) {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new BadCredentialsException("Refresh token không hợp lệ");
        }
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private BadCredentialsException invalidToken() {
        return new BadCredentialsException("Refresh token không hợp lệ");
    }

    private static String randomToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record TokenPair(String accessToken, String refreshToken,
                            LocalDateTime refreshExpiresAt, UserResponse user) { }
}
