package com.fashion.order.repository;

import com.fashion.order.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ReturnRequest> findByOrderIdAndUserIdOrderByCreatedAtDesc(Long orderId, Long userId);
    List<ReturnRequest> findAllByOrderByCreatedAtDesc();
    boolean existsByOrderItemIdAndStatusIn(Long orderItemId, List<String> statuses);
}
