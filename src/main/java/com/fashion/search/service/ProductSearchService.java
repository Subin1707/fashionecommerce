package com.fashion.search.service;

import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductVariant;
import com.fashion.product.repository.ProductImageRepository;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductVariantRepository;
import com.fashion.review.repository.ReviewRepository;
import com.fashion.search.document.ProductDocument;
import com.fashion.search.dto.ProductSearchRequest;
import com.fashion.search.dto.ProductSearchResponse;
import com.fashion.search.repository.ProductSearchRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public void syncProductsToElasticsearch() {
        log.info("Starting sync of products to Elasticsearch...");
        List<Product> allProducts = productRepository.findAll();
        
        List<ProductDocument> documents = convertToDocuments(allProducts);

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
                normalizeCategorySlug(request.getCategory()),
                lower(request.getCategoryName()),
                request.getCategoryId(),
                pattern(request.getStyle()),
                lower(request.getSize()),
                lower(request.getColor()),
                request.getMinPrice(),
                request.getMaxPrice(),
                pageable);
        List<Product> content = products.getContent();
        List<Long> ids = content.stream().map(Product::getId).toList();
        Map<Long, String> primaryImages = loadPrimaryImageUrls(ids);
        Map<Long, List<ProductVariant>> variants = new HashMap<>();
        if (!content.isEmpty()) {
            productVariantRepository.findByProductIn(content).stream()
                    .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()))
                    .forEach(variant -> variants.computeIfAbsent(variant.getProduct().getId(), ignored -> new ArrayList<>()).add(variant));
        }
        Map<Long, ReviewRepository.ProductRatingSummary> ratings = new HashMap<>();
        if (!ids.isEmpty()) {
            reviewRepository.summarizeProducts(ids).forEach(rating -> ratings.put(rating.getProductId(), rating));
        }
        return products.map(product -> convertToResponse(
                product,
                primaryImages.get(product.getId()),
                variants.getOrDefault(product.getId(), List.of()),
                ratings.get(product.getId())));
    }

    private ProductSearchResponse convertToResponse(
            Product product,
            String primaryImageUrl,
            List<ProductVariant> variants,
            ReviewRepository.ProductRatingSummary rating) {
        List<String> colors = variants.stream().map(ProductVariant::getColor).filter(java.util.Objects::nonNull).distinct().toList();
        List<String> sizes = variants.stream().map(ProductVariant::getSize).filter(java.util.Objects::nonNull).distinct().toList();
        int stock = variants.stream().map(ProductVariant::getStockQty).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        return ProductSearchResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .slug(product.getSlug())
                .basePrice(product.getBasePrice())
                .salePrice(product.getSalePrice() == null ? product.getBasePrice() : product.getSalePrice())
                .material(product.getMaterial())
                .fit(product.getFit())
                .gender(product.getGender())
                .status(product.getStatus())
                .brandName(product.getBrandName())
                .categoryName(product.getCategory() == null ? "" : product.getCategory().getName())
                .primaryImageUrl(primaryImageUrl)
                .colors(colors)
                .sizes(sizes)
                .totalStock(stock)
                .averageRating(rating == null ? 0.0 : rating.getAverageRating())
                .totalReviews(rating == null ? 0 : rating.getTotalReviews().intValue())
                .isFeatured(product.getIsFeatured())
                .isNew(product.getIsNew())
                .build();
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
        Map<Long, ReviewRepository.ProductRatingSummary> ratingSummaries = loadRatingSummaries(
                allTopRated.stream().map(ProductDocument::getId).toList());
        for (ProductDocument document : allTopRated) {
            ReviewRepository.ProductRatingSummary summary = ratingSummaries.get(document.getId());
            document.setAverageRating(summary == null ? 0.0 : summary.getAverageRating());
            document.setTotalReviews(summary == null ? 0 : summary.getTotalReviews().intValue());
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

        List<Product> activeProducts = products.stream()
                .filter(product -> product.getStatus() != null && "ACTIVE".equalsIgnoreCase(product.getStatus()))
                .toList();
        return convertToDocuments(activeProducts);
    }

    private List<ProductDocument> activeProductsFromDatabase() {
        List<Product> activeProducts = productRepository.findAll().stream()
                .filter(product -> product.getStatus() != null && "ACTIVE".equalsIgnoreCase(product.getStatus()))
                .toList();
        return convertToDocuments(activeProducts);
    }

    private double safeRating(Double rating) {
        return rating != null ? rating : 0.0;
    }

    private List<ProductDocument> convertToDocuments(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, String> primaryImageUrls = loadPrimaryImageUrls(productIds);
        Map<Long, List<ProductVariant>> variantsByProduct = loadVariants(products);

        Map<Long, ReviewRepository.ProductRatingSummary> ratingSummaries = loadRatingSummaries(productIds);

        return products.stream()
                .map(product -> convertToDocument(
                        product,
                        primaryImageUrls.get(product.getId()),
                        ratingSummaries.get(product.getId()),
                        variantsByProduct.getOrDefault(product.getId(), List.of())))
                .collect(Collectors.toList());
    }

    private Map<Long, List<ProductVariant>> loadVariants(List<Product> products) {
        Map<Long, List<ProductVariant>> variantsByProduct = new HashMap<>();
        List<Product> productsWithoutLoadedVariants = new ArrayList<>();

        for (Product product : products) {
            if (Hibernate.isInitialized(product.getVariants())) {
                variantsByProduct.put(product.getId(), product.getVariants());
            } else {
                productsWithoutLoadedVariants.add(product);
            }
        }

        for (int start = 0; start < productsWithoutLoadedVariants.size(); start += 500) {
            List<Product> batch = productsWithoutLoadedVariants.subList(
                    start,
                    Math.min(start + 500, productsWithoutLoadedVariants.size()));
            productVariantRepository.findByProductIn(batch).forEach(variant ->
                    variantsByProduct.computeIfAbsent(
                            variant.getProduct().getId(),
                            ignored -> new ArrayList<>())
                            .add(variant));
        }
        return variantsByProduct;
    }

    private Map<Long, String> loadPrimaryImageUrls(List<Long> productIds) {
        Map<Long, String> imageUrls = new HashMap<>();
        for (int start = 0; start < productIds.size(); start += 500) {
            List<Long> batch = productIds.subList(start, Math.min(start + 500, productIds.size()));
            productImageRepository.findSummariesByProductIdIn(batch).forEach(image -> {
                if (image.getImageUrl() == null || image.getImageUrl().isBlank()) return;
                if (image.getVariantId() == null && Boolean.TRUE.equals(image.getIsPrimary())) {
                    imageUrls.put(image.getProductId(), image.getImageUrl());
                } else if (!imageUrls.containsKey(image.getProductId()) && image.getVariantId() == null) {
                    imageUrls.put(image.getProductId(), image.getImageUrl());
                } else if (!imageUrls.containsKey(image.getProductId())) {
                    imageUrls.put(image.getProductId(), image.getImageUrl());
                }
            });
        }
        return imageUrls;
    }

    private Map<Long, ReviewRepository.ProductRatingSummary> loadRatingSummaries(List<Long> productIds) {
        Map<Long, ReviewRepository.ProductRatingSummary> summaries = new HashMap<>();
        for (int start = 0; start < productIds.size(); start += 500) {
            List<Long> batch = productIds.subList(start, Math.min(start + 500, productIds.size()));
            reviewRepository.summarizeProducts(batch)
                    .forEach(summary -> summaries.put(summary.getProductId(), summary));
        }
        return summaries;
    }

    private ProductDocument convertToDocument(
            Product product,
            String primaryImageUrl,
            ReviewRepository.ProductRatingSummary ratingSummary,
            List<ProductVariant> variants) {
        List<String> sizes = variants != null
                ? variants.stream()
                .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()))
                .map(ProductVariant::getSize)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList())
                : List.of();
        
        List<String> colors = variants != null
                ? variants.stream()
                .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()))
                .map(ProductVariant::getColor)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList())
                : List.of();

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
                .primaryImageUrl(primaryImageUrl)
                .colors(colors)
                .sizes(sizes)
                .totalStock(variants.stream()
                        .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()))
                        .map(ProductVariant::getStockQty)
                        .filter(java.util.Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum())
                .averageRating(ratingSummary == null ? 0.0 : ratingSummary.getAverageRating())
                .totalReviews(ratingSummary == null ? 0 : ratingSummary.getTotalReviews().intValue())
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

    private String normalizeCategorySlug(String value) {
        String normalized = normalize(value);
        if (normalized == null) return null;
        return java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private String lower(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private String pattern(String value) {
        String normalized = lower(value);
        return normalized == null ? null : "%" + normalized + "%";
    }


    public ProductSearchService(
            final ProductSearchRepository productSearchRepository,
            final ProductRepository productRepository,
            final ProductImageRepository productImageRepository,
            final ProductVariantRepository productVariantRepository,
            final ReviewRepository reviewRepository) {
        this.productSearchRepository = productSearchRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productVariantRepository = productVariantRepository;
        this.reviewRepository = reviewRepository;
    }
}
