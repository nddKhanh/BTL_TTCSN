package com.highlands.order.repository;

import com.highlands.order.model.ApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {
    Optional<ApprovalHistory> findByApproval_IdAndRequestId(Long approvalId, String requestId);
    List<ApprovalHistory> findByApproval_IdOrderByCreatedAtAscIdAsc(Long approvalId);
}
