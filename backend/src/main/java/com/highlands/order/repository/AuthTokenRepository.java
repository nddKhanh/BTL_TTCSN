package com.highlands.order.repository;

import com.highlands.order.model.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByTokenHashAndPurpose(String tokenHash, AuthToken.TokenPurpose purpose);
    void deleteByUserIdAndPurpose(Long userId, AuthToken.TokenPurpose purpose);
}
