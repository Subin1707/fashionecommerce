package com.fashion.order.repository;

import com.fashion.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from OrderItem i join fetch i.order where i.id = :id")
    java.util.Optional<OrderItem> findForReview(@org.springframework.data.repository.query.Param("id") Long id);
}
