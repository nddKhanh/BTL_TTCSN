package com.highlands.order.repository;

import com.highlands.order.model.FulfillmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FulfillmentHistoryRepository extends JpaRepository<FulfillmentHistory, Long> {
    Optional<FulfillmentHistory> findByOrder_IdAndRequestId(Long orderId, String requestId);
    List<FulfillmentHistory> findByOrder_IdOrderByCreatedAtAscIdAsc(Long orderId);
}
