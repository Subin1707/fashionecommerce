package com.fashion.shipping.service;

import java.util.List;

public interface RoutingService {
    record RoutePoint(double latitude, double longitude) {}
    record RouteResult(double distanceMeters, double durationSeconds, List<RoutePoint> points) {}
    RouteResult findRoute(double startLat, double startLng, double endLat, double endLng);
}
