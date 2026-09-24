package com.fashion.order.repository;

import com.fashion.order.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findForUpdate(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"items"})
    List<Order> findAll();

    @Override
    @EntityGraph(attributePaths = {"items"})
    Page<Order> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findById(Long id);

    @Query("select distinct o from Order o left join fetch o.items where o.userId = :userId order by o.createdAt desc")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("select distinct o from Order o left join fetch o.items where o.id = :id and o.userId = :userId")
    Optional<Order> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    
    @EntityGraph(attributePaths = {"items"})
    Page<Order> findByOrderStatus(String orderStatus, Pageable pageable);
    
    @EntityGraph(attributePaths = {"items"})
    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
    
    Long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    
    @Query("SELECT SUM(o.finalAmount) FROM Order o WHERE o.createdAt BETWEEN :from AND :to")
    Optional<BigDecimal> getTotalRevenueBetweenDates(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
