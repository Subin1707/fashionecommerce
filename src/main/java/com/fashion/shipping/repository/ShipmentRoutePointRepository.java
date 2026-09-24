package com.fashion.shipping.repository;

import com.fashion.shipping.entity.ShipmentRoutePoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRoutePointRepository extends JpaRepository<ShipmentRoutePoint, Long> {
    List<ShipmentRoutePoint> findByShipmentIdOrderByPointIndexAsc(Long shipmentId);
}
