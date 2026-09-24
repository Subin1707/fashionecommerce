package com.fashion.shipping.repository;

import com.fashion.shipping.entity.Shipment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    java.util.List<Shipment> findByStatusIn(java.util.Collection<String> statuses);
    Optional<Shipment> findByOrderId(Long orderId);
}