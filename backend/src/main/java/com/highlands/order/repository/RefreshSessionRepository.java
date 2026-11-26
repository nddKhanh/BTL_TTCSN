package com.highlands.order.repository;

import com.highlands.order.model.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshSession> findByTokenHash(String tokenHash);
    List<RefreshSession> findAllByUserIdAndRevokedAtIsNull(Long userId);
    List<RefreshSession> findAllByFamilyIdAndRevokedAtIsNull(String familyId);
}
