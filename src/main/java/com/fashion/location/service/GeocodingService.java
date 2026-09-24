package com.fashion.location.service;

import com.fashion.location.dto.LocationResult;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeocodingService {
    private final MapProviderClient client;
    private final String baseUrl;
    private final boolean photon;
    private long nextRequest;
    private record Cached(JsonNode value, long expiresAt) {}
    private final Map<String, Cached> cache = new LinkedHashMap<>();

    public GeocodingService(MapProviderClient client,
            @Value("${fashion.maps.geocoding-url:https://photon.komoot.io}") String baseUrl,
            @Value("${fashion.maps.geocoding-provider:photon}") String provider) {
        this.client = client;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        if (!Set.of("photon", "nominatim").contains(provider))
            throw new IllegalArgumentException("Unsupported geocoding provider: " + provider);
        this.photon = "photon".equals(provider);
    }

    public List<LocationResult> search(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 3 || q.length() > 200) throw new IllegalArgumentException("Nhập địa chỉ từ 3 đến 200 ký tự.");
        JsonNode rows = fetch((photon ? "/api/?limit=5&countrycode=VN&q="
                : "/search?format=jsonv2&limit=5&countrycodes=vn&accept-language=vi&q=")
                + URLEncoder.encode(q, StandardCharsets.UTF_8));
        if (photon) rows = rows.path("features");
        if (!rows.isArray()) throw new IllegalArgumentException("Dịch vụ tìm địa chỉ trả dữ liệu không hợp lệ.");
        List<LocationResult> results = new ArrayList<>();
        rows.forEach(row -> results.add(photon ? photonResult(row) : result(row)));
        return results;
    }

    public LocationResult reverse(double lat, double lng) {
        validate(lat, lng);
        JsonNode row = fetch(String.format(Locale.ROOT,
                photon ? "/reverse?limit=1&lat=%.6f&lon=%.6f"
                        : "/reverse?format=jsonv2&accept-language=vi&lat=%.6f&lon=%.6f", lat, lng));
        if (row.has("error")) throw new IllegalArgumentException("Không tìm thấy địa chỉ tại vị trí này.");
        if (photon && (!row.path("features").isArray() || row.path("features").isEmpty()))
            throw new IllegalArgumentException("Không tìm thấy địa chỉ tại vị trí này.");
        var found = photon ? photonResult(row.path("features").get(0)) : result(row);
        return new LocationResult(found.displayName(), lat, lng);
    }

    public static void validate(double lat, double lng) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180)
            throw new IllegalArgumentException("Tọa độ không hợp lệ.");
    }

    private LocationResult result(JsonNode row) {
        double lat = row.path("lat").asDouble(Double.NaN), lng = row.path("lon").asDouble(Double.NaN);
        validate(lat, lng);
        String name = row.path("display_name").asText("");
        if (name.isBlank()) throw new IllegalArgumentException("Không tìm thấy địa chỉ.");
        return new LocationResult(name, lat, lng);
    }

    private LocationResult photonResult(JsonNode row) {
        JsonNode coordinates = row.path("geometry").path("coordinates");
        double lng = coordinates.path(0).asDouble(Double.NaN);
        double lat = coordinates.path(1).asDouble(Double.NaN);
        validate(lat, lng);
        JsonNode properties = row.path("properties");
        Set<String> parts = new LinkedHashSet<>();
        String street = (properties.path("housenumber").asText("") + " "
                + properties.path("street").asText("")).trim();
        String name = properties.path("name").asText("").trim();
        if (!name.isBlank()) parts.add(name);
        if (!street.isBlank()) parts.add(street);
        for (String key : List.of("district", "city", "county", "state", "country")) {
            String part = properties.path(key).asText("").trim();
            if (!part.isBlank()) parts.add(part);
        }
        if (parts.isEmpty()) throw new IllegalArgumentException("Không tìm thấy địa chỉ.");
        return new LocationResult(String.join(", ", parts), lat, lng);
    }

    // One in-flight request and at most one request/second across all users of this instance.
    private synchronized JsonNode fetch(String path) {
        long now = System.currentTimeMillis();
        Cached hit = cache.get(path);
        if (hit != null && hit.expiresAt() > now) return hit.value();
        if (now < nextRequest) throw new IllegalArgumentException("Dịch vụ tìm địa chỉ đang bận, vui lòng thử lại sau một giây.");
        nextRequest = now + 1100;
        JsonNode value = client.get(baseUrl + path);
        if (cache.size() >= 1000) cache.remove(cache.keySet().iterator().next());
        cache.put(path, new Cached(value, now + 86_400_000));
        return value;
    }
}
