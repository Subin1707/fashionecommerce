package com.fashion.style.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.product.entity.Product;
import com.fashion.product.repository.ProductRepository;
import com.fashion.search.dto.ProductSearchResponse;
import com.fashion.style.dto.VisualSearchResponse;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class VisualSearchService {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Value("${fashion.visual-search.enabled:false}")
    private boolean enabled;

    @Value("${fashion.visual-search.clip-url:http://localhost:8000/embed}")
    private String clipUrl;

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUrl;

    public VisualSearchResponse search(MultipartFile image, int limit) {
        if (!enabled) {
            return VisualSearchResponse.builder()
                    .enabled(false)
                    .message("Visual search chưa được bật. Core ecommerce vẫn hoạt động bình thường.")
                    .products(List.of())
                    .build();
        }
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Ảnh tìm kiếm không được để trống");
        }
        if (image.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Ảnh tìm kiếm không được vượt quá 10MB");
        }

        try {
            float[] embedding = requestEmbedding(image);
            List<Long> productIds = vectorSearch(embedding, Math.max(1, Math.min(limit, 50)));
            Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));
            List<ProductSearchResponse> products = productIds.stream()
                    .map(productsById::get)
                    .filter(product -> product != null && "ACTIVE".equalsIgnoreCase(product.getStatus()))
                    .map(this::toResponse)
                    .toList();
            return VisualSearchResponse.builder()
                    .enabled(true)
                    .message("Đã tìm thấy sản phẩm tương tự.")
                    .products(products)
                    .build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Visual search provider không khả dụng", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Visual search provider không khả dụng", exception);
        }
    }

    private float[] requestEmbedding(MultipartFile image) throws IOException, InterruptedException {
        String boundary = "----FashionVisualSearch";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"image\"; filename=\"image\"\r\n"
            + "Content-Type: " + (image.getContentType() == null ? "application/octet-stream" : image.getContentType())
            + "\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        body.write(image.getBytes());
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(clipUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("CLIP provider returned HTTP " + response.statusCode());
        }
        JsonNode vector = objectMapper.readTree(response.body()).path("embedding");
        float[] embedding = new float[vector.size()];
        for (int index = 0; index < vector.size(); index++) {
            embedding[index] = (float) vector.get(index).asDouble();
        }
        if (embedding.length == 0) throw new IOException("CLIP response does not contain embedding");
        return embedding;
    }

    private List<Long> vectorSearch(float[] embedding, int limit) throws IOException, InterruptedException {
        String vector = objectMapper.writeValueAsString(embedding);
        String body = "{\"knn\":{\"field\":\"embedding\",\"query_vector\":" + vector
                + ",\"k\":" + limit + ",\"num_candidates\":" + Math.max(100, limit * 10) + "}}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(elasticsearchUrl + "/products/_search"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw new IOException("Elasticsearch returned HTTP " + response.statusCode());
        List<Long> ids = new ArrayList<>();
        for (JsonNode hit : objectMapper.readTree(response.body()).path("hits").path("hits")) {
            ids.add(hit.path("_id").asLong());
        }
        return ids;
    }

    private ProductSearchResponse toResponse(Product product) {
        return ProductSearchResponse.builder()
                .id(product.getId()).name(product.getName()).description(product.getDescription())
                .slug(product.getSlug()).basePrice(product.getBasePrice()).salePrice(product.getDisplayPrice())
                .material(product.getMaterial()).fit(product.getFit()).gender(product.getGender())
                .status(product.getStatus()).brandName(product.getBrandName())
                .categoryName(product.getCategory() == null ? null : product.getCategory().getName())
                .build();
    }
}
