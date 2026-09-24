package com.fashion.shipping.service;

import com.fashion.order.repository.OrderRepository;
import com.fashion.shipping.repository.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@EnableScheduling
@ConditionalOnProperty(name = "fashion.shipping.simulator.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ShipmentSimulatorService {
    private final ShipmentRepository shipments;
    private final ShipmentRoutePointRepository points;
    private final OrderRepository orders;
    private final ShipmentService service;
    private final TransactionTemplate transactions;
    private static final List<String> ACTIVE = List.of("PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY");

    @Scheduled(fixedDelayString = "${fashion.shipping.simulator.interval-ms:5000}")
    public void tick() {
        for (var shipment : shipments.findByStatusIn(ACTIVE)) {
            try { advance(shipment.getOrderId()); }
            catch (RuntimeException ex) { log.warn("Cannot advance shipment {}: {}", shipment.getId(), ex.getMessage()); }
        }
    }

    public void advance(Long orderId) {
        transactions.executeWithoutResult(transaction -> {
            var order = orders.findForUpdate(orderId).orElse(null);
            if (order == null || !"SHIPPING".equals(order.getOrderStatus())) return;
            var shipment = shipments.findByOrderId(orderId).orElse(null);
            if (shipment == null || !ACTIVE.contains(shipment.getStatus())) return;
            var route = points.findByShipmentIdOrderByPointIndexAsc(shipment.getId());
            if (route.size() < 2) return; // Legacy shipments never get a fabricated straight route.
            int index = Math.min(shipment.getRouteIndex() + 1, route.size() - 1);
            var point = route.get(index);
            shipment.setRouteIndex(index);
            shipment.setCurrentLat(point.getLatitude());
            shipment.setCurrentLng(point.getLongitude());
            shipments.save(shipment);
            double progress = (double) index / (route.size() - 1);
            String target = switch (shipment.getStatus()) {
                case "PICKED_UP" -> progress >= .10 ? "IN_TRANSIT" : null;
                case "IN_TRANSIT" -> progress >= .80 ? "OUT_FOR_DELIVERY" : null;
                case "OUT_FOR_DELIVERY" -> progress >= 1 ? "DELIVERED" : null;
                default -> null;
            };
            if (target != null) service.update(orderId, new ShipmentService.UpdateRequest(target, null));
        });
    }
}
