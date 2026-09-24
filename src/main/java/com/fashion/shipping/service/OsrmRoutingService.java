package com.fashion.shipping.service;

import com.fashion.location.service.GeocodingService;
import com.fashion.location.service.MapProviderClient;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OsrmRoutingService implements RoutingService {
    private final MapProviderClient client;
    private final String baseUrl;

    public OsrmRoutingService(MapProviderClient client,
            @Value("${fashion.maps.routing-url:https://router.project-osrm.org}") String baseUrl) {
        this.client = client;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    public RouteResult findRoute(double startLat, double startLng, double endLat, double endLng) {
        GeocodingService.validate(startLat, startLng);
        GeocodingService.validate(endLat, endLng);
        var response = client.get(baseUrl + "/route/v1/driving/" + startLng + "," + startLat
                + ";" + endLng + "," + endLat + "?overview=full&geometries=geojson&steps=false");
        if (!"Ok".equals(response.path("code").asText()))
            throw new IllegalArgumentException("Không tìm được tuyến đường giao hàng. Vui lòng kiểm tra vị trí.");
        var route = response.path("routes").path(0);
        var points = new ArrayList<RoutePoint>();
        for (var point : route.path("geometry").path("coordinates")) {
            double lat = point.path(1).asDouble(Double.NaN), lng = point.path(0).asDouble(Double.NaN);
            GeocodingService.validate(lat, lng);
            points.add(new RoutePoint(lat, lng));
        }
        if (points.size() < 2) throw new IllegalArgumentException("Tuyến đường chưa có đủ dữ liệu.");
        return new RouteResult(route.path("distance").asDouble(), route.path("duration").asDouble(), java.util.List.copyOf(points));
    }

}
