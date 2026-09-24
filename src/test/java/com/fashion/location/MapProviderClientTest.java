package com.fashion.location;

import com.fashion.location.service.MapProviderClient;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapProviderClientTest {
    @Test
    void retriesTransientFailureAndReturnsJson() throws Exception {
        var calls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/search", exchange -> {
            int status = calls.incrementAndGet() == 1 ? 503 : 200;
            byte[] body = "[{\"display_name\":\"CT4 Yen Nghia\"}]".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var result = new MapProviderClient("FashionShopTest/1.0")
                    .get("http://127.0.0.1:" + server.getAddress().getPort() + "/search");
            assertThat(result.get(0).path("display_name").asText()).isEqualTo("CT4 Yen Nghia");
            assertThat(calls.get()).isEqualTo(2);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void interruptedRequestPreservesInterruptFlag() {
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> new MapProviderClient("FashionShopTest/1.0")
                    .get("http://127.0.0.1:1/search"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
