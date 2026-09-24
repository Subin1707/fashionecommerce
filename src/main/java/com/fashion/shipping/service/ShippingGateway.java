package com.fashion.shipping.service;

import com.fashion.order.entity.Order;
import com.fashion.shipping.entity.ShippingProvider;
import com.fashion.shipping.entity.Shipment;
import java.util.List;

/** Boundary for a future carrier API adapter. Providers are business data, never users. */
public interface ShippingGateway {
    record RoutePoint(double latitude, double longitude, String location) {}

    String createTrackingCode(ShippingProvider provider, Order order, double weightKg, double fee);

    List<RoutePoint> route(Shipment shipment);

    RoutePoint nextPoint(Shipment shipment, int routeIndex);
}
