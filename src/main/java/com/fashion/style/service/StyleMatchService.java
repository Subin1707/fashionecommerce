package com.fashion.style.service;

import com.fashion.product.entity.Product;
import com.fashion.product.dto.ProductResponse;
import com.fashion.product.entity.ProductImage;
import com.fashion.product.repository.ProductImageRepository;
import com.fashion.product.repository.ProductRepository;
import com.fashion.style.dto.StyleMatchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StyleMatchService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> recommendProducts(StyleMatchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu phân tích style không hợp lệ");
        }

        String color = request.getColor() == null ? "" : request.getColor().toLowerCase();
        String style = request.getStyle() == null ? "" : request.getStyle().toLowerCase();
        String category = request.getCategory() == null ? "" : request.getCategory().toLowerCase();
        String pattern = request.getPattern() == null ? "" : request.getPattern().toLowerCase();
        String fashionType = request.getFashionType() == null ? "" : request.getFashionType().toLowerCase();

        return productRepository.findAll().stream()
                .filter(product -> product.getStatus() != null && "ACTIVE".equalsIgnoreCase(product.getStatus()))
                .filter(product -> matchScore(product, color, style, category, pattern, fashionType) > 0)
                .map(this::toResponse)
                .limit(10)
                .toList();
    }

            private ProductResponse toResponse(Product product) {
            List<String> imageUrls = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId()).stream()
                .sorted((left, right) -> Integer.compare(
                    left.getDisplayOrder() == null ? 0 : left.getDisplayOrder(),
                    right.getDisplayOrder() == null ? 0 : right.getDisplayOrder()))
                .map(ProductImage::getImageUrl)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
            return ProductResponse.fromEntity(product, imageUrls.isEmpty() ? null : imageUrls.getFirst(), imageUrls);
            }

    private int matchScore(Product product, String color, String style, String category, String pattern, String fashionType) {
        int score = 0;
        String name = product.getName();
        String description = product.getDescription();
        String brandName = product.getBrand() == null ? null : product.getBrand().getName();
        String categoryName = product.getCategory() == null ? null : product.getCategory().getName();

        if (!color.isBlank() && (matches(name, color) || matches(description, color) || matches(brandName, color))) {
            score++;
        }
        if (!style.isBlank() && (matches(name, style) || matches(description, style) || matches(product.getFit(), style))) {
            score++;
        }
        if (!category.isBlank() && (matches(name, category) || matches(categoryName, category))) {
            score++;
        }
        if (!pattern.isBlank() && matches(description, pattern)) {
            score++;
        }
        if (!fashionType.isBlank() && (matches(product.getGender(), fashionType) || matches(description, fashionType))) {
            score++;
        }
        return score;
    }

    private boolean matches(String productValue, String desiredValue) {
        if (productValue == null) {
            return false;
        }
        String normalizedProduct = productValue.toLowerCase();
        return normalizedProduct.contains(desiredValue) || desiredValue.contains(normalizedProduct);
    }
}
