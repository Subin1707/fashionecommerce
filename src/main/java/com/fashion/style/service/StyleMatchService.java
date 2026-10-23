package com.fashion.style.service;

import com.fashion.product.entity.Product;
import com.fashion.product.dto.ProductResponse;
import com.fashion.product.entity.ProductImage;
import com.fashion.product.repository.ProductImageRepository;
import com.fashion.product.repository.ProductRepository;
import com.fashion.style.dto.StyleMatchRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StyleMatchService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    @Transactional(readOnly = true)
    public List<ProductResponse> recommendProducts(StyleMatchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu phân tích style không hợp lệ");
        }

        String color = normalize(request.getColor());
        String style = normalize(request.getStyle());
        String category = normalize(request.getCategory());
        String pattern = normalize(request.getPattern());
        String fashionType = normalize(request.getFashionType());

        List<Product> activeProducts = productRepository.findByStatusIgnoreCase("ACTIVE");
        Map<Long, List<ProductImage>> imagesByProduct = productImageRepository.findByProductIn(activeProducts).stream()
                .collect(Collectors.groupingBy(image -> image.getProduct().getId()));
        List<Product> ranked = activeProducts.stream()
                .sorted((left, right) -> Integer.compare(
                        matchScore(right, color, style, category, pattern, fashionType),
                        matchScore(left, color, style, category, pattern, fashionType)))
                .toList();
        List<Product> matches = ranked.stream()
                .filter(product -> matchScore(product, color, style, category, pattern, fashionType) > 0)
                .toList();
        // Catalog attributes are often incomplete. Still return real active products
        // so the budget and disliked-color filters on the client can make useful picks.
        return (matches.isEmpty() ? ranked : matches).stream()
                .map(product -> toResponse(product, imagesByProduct.getOrDefault(product.getId(), List.of())))
                .limit(10)
                .toList();
    }

    private ProductResponse toResponse(Product product, List<ProductImage> images) {
            List<String> imageUrls = images.stream()
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

        boolean variantColorMatches = !color.isBlank() && product.getVariants().stream()
                .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()) && variant.getStockQty() != null && variant.getStockQty() > 0)
                .anyMatch(variant -> matchesColor(variant.getColor(), color));
        if (!color.isBlank() && (variantColorMatches || matchesColor(name, color) || matchesColor(description, color))) {
            score++;
        }
        if (!style.isBlank() && (matches(name, style) || matches(description, style) || matches(product.getFit(), style)
                || matchesStyle(name, style) || matchesStyle(description, style) || matchesStyle(product.getFit(), style))) {
            score++;
        }
        if (!category.isBlank() && (matches(name, category) || matches(categoryName, category))) {
            score++;
        }
        if (!pattern.isBlank() && matches(description, pattern)) {
            score++;
        }
        if (!fashionType.isBlank() && !"unisex".equals(fashionType)
                && (matches(product.getGender(), fashionType) || matches(description, fashionType))) {
            score++;
        }
        return score;
    }

    private boolean matchesColor(String value, String desired) {
        String text = normalize(value);
        if (matches(text, desired)) return true;
        if (desired.contains("trang") || desired.contains("be")) return text.contains("white") || text.contains("cream") || text.contains("beige") || text.contains("ivory");
        if (desired.contains("den")) return text.contains("black");
        if (desired.contains("xanh")) return text.contains("blue") || text.contains("green") || text.contains("navy");
        if (desired.contains("pastel")) return text.contains("pastel") || text.contains("pink") || text.contains("lavender");
        if (desired.contains("noi")) return text.contains("red") || text.contains("orange") || text.contains("yellow");
        return false;
    }

    private boolean matchesStyle(String value, String style) {
        String text = normalize(value);
        return switch (style) {
            case "minimal", "basic" -> text.contains("basic") || text.contains("minimal") || text.contains("toi gian") || text.contains("tron");
            case "streetwear" -> text.contains("street") || text.contains("hoodie") || text.contains("oversize") || text.contains("casual");
            case "vintage" -> text.contains("vintage") || text.contains("retro") || text.contains("classic");
            case "elegant" -> text.contains("elegant") || text.contains("formal") || text.contains("thanh lich") || text.contains("cong so");
            case "sporty" -> text.contains("sport") || text.contains("the thao") || text.contains("active");
            case "y2k" -> text.contains("y2k") || text.contains("crop") || text.contains("baggy");
            default -> false;
        };
    }

    private boolean matches(String productValue, String desiredValue) {
        if (productValue == null) {
            return false;
        }
        String normalizedProduct = normalize(productValue);
        String normalizedDesired = normalize(desiredValue);
        return normalizedProduct.contains(normalizedDesired) || normalizedDesired.contains(normalizedProduct);
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'd')
                .toLowerCase();
        return normalized;
    }

    public StyleMatchService(final ProductRepository productRepository, final ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
    }
}
