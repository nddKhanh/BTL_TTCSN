package com.highlands.order.repository;

import com.highlands.order.model.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);
    List<RefreshSession> findAllByUserIdAndRevokedAtIsNull(Long userId);
    List<RefreshSession> findAllByFamilyIdAndRevokedAtIsNull(String familyId);
}
