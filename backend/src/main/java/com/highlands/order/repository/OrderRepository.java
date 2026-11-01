package com.highlands.order.repository;

import com.highlands.order.model.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import com.highlands.order.model.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByOrderCode(String orderCode);

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndUserEmailIgnoreCase(Long id, String email);

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("""
            select distinct orders
            from Order orders
            where (:status is null or orders.status = :status)
              and (:keyword is null
                   or lower(orders.orderCode) like lower(concat('%', :keyword, '%'))
                   or lower(orders.customerName) like lower(concat('%', :keyword, '%'))
                   or orders.customerPhone like concat('%', :keyword, '%'))
              and (:createdFrom is null or orders.createdAt >= :createdFrom)
              and (:createdUntil is null or orders.createdAt < :createdUntil)
            order by orders.createdAt desc
            """)
    List<Order> searchForAdmin(
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdUntil") LocalDateTime createdUntil
    );

    long countByStatus(OrderStatus status);

    List<Order> findTop5ByOrderByCreatedAtDesc();

    List<Order> findByStatus(OrderStatus status);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.status = :status and o.createdAt >= :from and o.createdAt < :until")
    BigDecimal sumTotalAmountByStatusAndCreatedAtBetween(@Param("status") OrderStatus status,
                                                         @Param("from") LocalDateTime from,
                                                         @Param("until") LocalDateTime until);

    long countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(OrderStatus status, LocalDateTime from, LocalDateTime until);
}
