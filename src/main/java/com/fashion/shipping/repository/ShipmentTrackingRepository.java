package com.fashion.shipping.repository;

import com.fashion.shipping.entity.ShipmentTracking;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {
    List<ShipmentTracking> findByShipmentIdOrderByCreatedAtAscIdAsc(Long shipmentId);
}