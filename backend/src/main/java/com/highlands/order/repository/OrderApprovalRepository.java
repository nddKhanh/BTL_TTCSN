package com.highlands.order.repository;

import com.highlands.order.model.OrderApproval;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderApprovalRepository extends JpaRepository<OrderApproval, Long> {

    @EntityGraph(attributePaths = {"order", "order.user"})
    Optional<OrderApproval> findByApprovalRef(String approvalRef);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Optional<OrderApproval> findByOrder_Id(Long orderId);

    List<OrderApproval> findByOrder_IdIn(Collection<Long> orderIds);
}
