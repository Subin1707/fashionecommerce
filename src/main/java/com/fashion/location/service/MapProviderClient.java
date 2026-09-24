package com.fashion.location.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MapProviderClient {
    private static final int MAX_ATTEMPTS = 3;
    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String userAgent;

    public MapProviderClient(@Value("${fashion.maps.user-agent:FashionShopDemo/1.0}") String userAgent) {
        this.userAgent = userAgent;
    }

    public JsonNode get(String url) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                        .header("User-Agent", userAgent).header("Accept", "application/json").GET().build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) return mapper.readTree(response.body());
                if (!isTransient(response.statusCode()) || attempt == MAX_ATTEMPTS)
                    throw new IllegalArgumentException("Dịch vụ bản đồ đang bận. Vui lòng thử lại.");
                pause(attempt);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalArgumentException("Yêu cầu bản đồ bị gián đoạn.", ex);
            } catch (java.io.IOException ex) {
                if (attempt == MAX_ATTEMPTS)
                    throw new IllegalArgumentException("Không kết nối được dịch vụ bản đồ. Vui lòng thử lại.", ex);
                pause(attempt);
            }
        }
        throw new IllegalArgumentException("Không kết nối được dịch vụ bản đồ. Vui lòng thử lại.");
    }

    private boolean isTransient(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private void pause(int attempt) {
        try {
            Thread.sleep(attempt * 350L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Yêu cầu bản đồ bị gián đoạn.", ex);
        }
    }
}
