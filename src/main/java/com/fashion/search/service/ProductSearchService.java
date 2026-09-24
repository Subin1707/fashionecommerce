package com.fashion.search.service;

import com.fashion.product.entity.Product;
import com.fashion.product.repository.ProductRepository;
import com.fashion.review.repository.ReviewRepository;
import com.fashion.search.document.ProductDocument;
import com.fashion.search.dto.ProductSearchRequest;
import com.fashion.search.dto.ProductSearchResponse;
import com.fashion.search.repository.ProductSearchRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public void syncProductsToElasticsearch() {
        log.info("Starting sync of products to Elasticsearch...");
        List<Product> allProducts = productRepository.findAll();
        
        List<ProductDocument> documents = allProducts.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());

        try {
            productSearchRepository.saveAll(documents);
            log.info("Synced {} products to Elasticsearch", documents.size());
        } catch (RuntimeException exception) {
            log.warn("Elasticsearch is unavailable; skipped product sync: {}", exception.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<ProductSearchResponse> searchProducts(ProductSearchRequest request, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.searchActiveProducts(
                pattern(request.getKeyword()),
                lower(request.getCategory()),
                pattern(request.getCategory()),
                pattern(request.getStyle()),
                lower(request.getSize()),
                lower(request.getColor()),
                request.getMinPrice(),
                request.getMaxPrice(),
                pageable);
        return products.map(product -> convertToResponse(convertToDocument(product)));
    }

    @Transactional(readOnly = true)
    public Page<ProductSearchResponse> getTopRatedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<ProductDocument> allTopRated;
        try {
            allTopRated = productSearchRepository.findByStatusOrderByAverageRatingDesc("ACTIVE");
        } catch (RuntimeException exception) {
            log.warn("Elasticsearch is unavailable; falling back to database top-rated products: {}", exception.getMessage());
            allTopRated = activeProductsFromDatabase().stream()
                    .sorted((left, right) -> Double.compare(
                            safeRating(right.getAverageRating()),
                            safeRating(left.getAverageRating())))
                    .collect(Collectors.toList());
        }
        for (ProductDocument document : allTopRated) {
            var summary = reviewRepository.summarize(document.getId());
            document.setAverageRating(summary.getAverageRating());
            document.setTotalReviews(summary.getTotalReviews().intValue());
        }
        allTopRated.sort((left, right) -> Double.compare(safeRating(right.getAverageRating()), safeRating(left.getAverageRating())));
        List<ProductDocument> topRated = allTopRated
                .stream()
                .limit((page + 1L) * size)
                .skip((long) page * size)
                .collect(Collectors.toList());
        
        List<ProductSearchResponse> responses = topRated.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, allTopRated.size());
    }

    private List<ProductDocument> findSearchDocuments(ProductSearchRequest request) {
        try {
            List<ProductDocument> documents;
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String keyword = request.getKeyword().trim();
                documents = productSearchRepository.findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(
                        keyword, keyword);
            } else {
                documents = productSearchRepository.findByStatus("ACTIVE");
            }
            if (documents.isEmpty()) {
                return searchProductsFromDatabase(request);
            }
            return documents;
        } catch (RuntimeException exception) {
            log.warn("Elasticsearch is unavailable; falling back to database product search: {}", exception.getMessage());
            return searchProductsFromDatabase(request);
        }
    }

    private List<ProductDocument> searchProductsFromDatabase(ProductSearchRequest request) {
        String keyword = request.getKeyword() == null ? "" : request.getKeyword().trim();
        List<Product> products = keyword.isEmpty()
                ? productRepository.findAll()
                : productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);

        return products.stream()
                .filter(product -> product.getStatus() != null && "ACTIVE".equalsIgnoreCase(product.getStatus()))
                .map(this::convertToDocument)
                .collect(Collectors.toList());
    }

    private List<ProductDocument> activeProductsFromDatabase() {
        return productRepository.findAll().stream()
                .filter(product -> product.getStatus() != null && "ACTIVE".equalsIgnoreCase(product.getStatus()))
                .map(this::convertToDocument)
                .collect(Collectors.toList());
    }

    private double safeRating(Double rating) {
        return rating != null ? rating : 0.0;
    }

    private ProductDocument convertToDocument(Product product) {
        List<String> sizes = product.getVariants() != null
                ? product.getVariants().stream()
                .map(v -> v.getSize())
                .distinct()
                .collect(Collectors.toList())
                : List.of();
        
        List<String> colors = product.getVariants() != null
                ? product.getVariants().stream()
                .map(v -> v.getColor())
                .distinct()
                .collect(Collectors.toList())
                : List.of();

        Double avgRating = 0.0;
        Integer totalReviews = 0;
        try {
            var summary = reviewRepository.summarize(product.getId());
            avgRating = summary.getAverageRating();
            totalReviews = summary.getTotalReviews().intValue();
        } catch (Exception e) {
            log.warn("Could not fetch reviews for product {}: {}", product.getId(), e.getMessage());
        }

        return ProductDocument.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .slug(product.getSlug())
                .basePrice(product.getBasePrice())
                .salePrice(product.getSalePrice())
                .material(product.getMaterial())
                .fit(product.getFit())
                .gender(product.getGender())
                .status(product.getStatus())
                .brandName(product.getBrandName())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "")
                .primaryImageUrl(product.getPrimaryImageUrl())
                .colors(colors)
                .sizes(sizes)
                .totalStock(product.getAvailableQuantity())
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .isFeatured(product.getIsFeatured())
                .isNew(product.getIsNew())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductSearchResponse convertToResponse(ProductDocument doc) {
        return ProductSearchResponse.builder()
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .slug(doc.getSlug())
                .basePrice(doc.getBasePrice())
                .salePrice(doc.getSalePrice() != null ? doc.getSalePrice() : doc.getBasePrice())
                .material(doc.getMaterial())
                .fit(doc.getFit())
                .gender(doc.getGender())
                .status(doc.getStatus())
                .brandName(doc.getBrandName())
                .categoryName(doc.getCategoryName())
                .primaryImageUrl(doc.getPrimaryImageUrl())
                .colors(doc.getColors())
                .sizes(doc.getSizes())
                .totalStock(doc.getTotalStock() == null ? 0 : doc.getTotalStock())
                .averageRating(doc.getAverageRating())
                .totalReviews(doc.getTotalReviews())
                .isFeatured(doc.getIsFeatured())
                .isNew(doc.getIsNew())
                .build();
    }

    private boolean filterBySize(ProductDocument doc, String size) {
        if (size == null || size.trim().isEmpty()) {
            return true;
        }
        return doc.getSizes() != null && doc.getSizes().stream()
                .anyMatch(s -> s.equalsIgnoreCase(size.trim()));
    }

    private boolean filterByColor(ProductDocument doc, String color) {
        if (color == null || color.trim().isEmpty()) {
            return true;
        }
        return doc.getColors() != null && doc.getColors().stream()
                .anyMatch(c -> c.equalsIgnoreCase(color.trim()));
    }

    private boolean filterByCategory(ProductDocument doc, String category) {
        if (category == null || category.trim().isEmpty()) {
            return true;
        }
        if ("TOP".equalsIgnoreCase(category)) {
            return containsAny(doc.getCategoryName(), "top", "ao", "shirt", "tee")
                    || containsAny(doc.getName(), "top", "ao", "shirt", "tee");
        }
        return containsIgnoreCase(doc.getCategoryName(), category)
                || containsIgnoreCase(doc.getName(), category);
    }

    private boolean filterByStyle(ProductDocument doc, String style) {
        if (style == null || style.trim().isEmpty()) {
            return true;
        }
        if ("OFFICE".equalsIgnoreCase(style)) {
            return containsAny(doc.getName(), "office", "cong so", "so mi", "shirt", "formal")
                    || containsAny(doc.getDescription(), "office", "cong so", "so mi", "formal")
                    || containsAny(doc.getFit(), "office", "formal");
        }
        return containsIgnoreCase(doc.getName(), style)
                || containsIgnoreCase(doc.getDescription(), style)
                || containsIgnoreCase(doc.getFit(), style)
                || containsIgnoreCase(doc.getMaterial(), style);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query.trim().toLowerCase());
    }

    private boolean containsAny(String value, String... queries) {
        for (String query : queries) {
            if (containsIgnoreCase(value, query)) return true;
        }
        return false;
    }

    private boolean filterByPriceRange(ProductDocument doc, BigDecimal minPrice, BigDecimal maxPrice) {
        BigDecimal price = doc.getSalePrice() != null ? doc.getSalePrice() : doc.getBasePrice();
        
        if (minPrice != null && price.compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            return false;
        }
        return true;
    }

    private boolean filterByRating(ProductDocument doc, Double minRating) {
        if (minRating == null || minRating <= 0) {
            return true;
        }
        return doc.getAverageRating() != null && doc.getAverageRating() >= minRating;
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String lower(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private String pattern(String value) {
        String normalized = lower(value);
        return normalized == null ? null : "%" + normalized + "%";
    }

}
