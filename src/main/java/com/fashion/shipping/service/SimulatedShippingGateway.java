package com.fashion.shipping.service;

import com.fashion.order.entity.Order;
import com.fashion.shipping.entity.ShippingProvider;
import com.fashion.shipping.entity.Shipment;
import org.springframework.stereotype.Component;
import com.fashion.shipping.repository.ShipmentRoutePointRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SimulatedShippingGateway implements ShippingGateway {
    private final ShipmentRoutePointRepository points;
    @Override
    public String createTrackingCode(ShippingProvider provider, Order order, double weightKg, double fee) {
        return provider.getCode() + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    @Override
    public List<RoutePoint> route(Shipment shipment) {
        return points.findByShipmentIdOrderByPointIndexAsc(shipment.getId()).stream()
                .map(p -> new RoutePoint(p.getLatitude(), p.getLongitude(), "Trên tuyến giao hàng"))
                .toList();
    }

    @Override
    public RoutePoint nextPoint(Shipment shipment, int routeIndex) {
        List<RoutePoint> route = route(shipment);
        int nextIndex = Math.min(routeIndex + 1, route.size() - 1);
        return route.get(nextIndex);
    }
}
