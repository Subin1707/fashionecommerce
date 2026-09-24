package com.fashion.admin.service;

import com.fashion.admin.dto.AdminProductDto;
import com.fashion.admin.dto.AdminProductVariantDto;
import com.fashion.admin.dto.CreateProductRequest;
import com.fashion.admin.dto.CreateVariantRequest;
import com.fashion.admin.dto.ProductImageDto;
import com.fashion.admin.dto.ProductSizeChartDto;
import com.fashion.admin.dto.UpdateProductRequest;
import com.fashion.admin.dto.UpdateVariantRequest;
import com.fashion.brand.entity.Brand;
import com.fashion.brand.repository.BrandRepository;
import com.fashion.category.entity.Category;
import com.fashion.category.repository.CategoryRepository;
import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductImage;
import com.fashion.product.entity.ProductSizeChart;
import com.fashion.product.entity.ProductVariant;
import com.fashion.product.repository.ProductImageRepository;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductSizeChartRepository;
import com.fashion.product.repository.ProductVariantRepository;
import com.fashion.search.service.ProductSearchService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.text.Normalizer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSizeChartRepository productSizeChartRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductSearchService productSearchService;
    private final EntityManager entityManager;

    public Page<AdminProductDto> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findAll(pageable).map(this::convertToAdminDto);
    }

    public AdminProductDto getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return convertToAdminDto(product);
    }

    public AdminProductDto createProduct(CreateProductRequest request) {
        validateProductRequest(request);

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new RuntimeException("Brand not found"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = Product.builder()
                .brand(brand)
                .category(category)
                .name(request.getName())
                .slug(uniqueSlug(request.getName(), null))
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .salePrice(request.getSalePrice())
                .material(request.getMaterial())
                .fit(request.getFit())
                .gender(request.getGender())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .isNew(request.getIsNew() != null ? request.getIsNew() : true)
                .build();

        Product saved = productRepository.saveAndFlush(product);
        createInitialVariants(saved, request);
        replaceProductImages(saved, request.getImages());
        replaceProductSizeCharts(saved, request.getSizeCharts());
        productSearchService.syncProductsToElasticsearch();
        return convertToAdminDto(saved);
    }

    public AdminProductDto updateProduct(Long productId, UpdateProductRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Product request is required");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Brand not found"));
            product.setBrand(brand);
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }

        if (request.getName() != null) {
            product.setName(request.getName());
            product.setSlug(uniqueSlug(request.getName(), productId));
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (request.getBasePrice() != null) {
            product.setBasePrice(request.getBasePrice());
        }

        if (request.getSalePrice() != null) {
            product.setSalePrice(request.getSalePrice());
        }

        if (request.getMaterial() != null) {
            product.setMaterial(request.getMaterial());
        }

        if (request.getFit() != null) {
            product.setFit(request.getFit());
        }

        if (request.getGender() != null) {
            product.setGender(request.getGender());
        }

        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        if (request.getIsFeatured() != null) {
            product.setIsFeatured(request.getIsFeatured());
        }

        if (request.getIsNew() != null) {
            product.setIsNew(request.getIsNew());
        }

        Product updated = productRepository.saveAndFlush(product);
        if (request.getImages() != null) {
            replaceProductImages(updated, request.getImages());
        }
        if (request.getSizeCharts() != null) {
            replaceProductSizeCharts(updated, request.getSizeCharts());
        }
        productSearchService.syncProductsToElasticsearch();
        return convertToAdminDto(updated);
    }

    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Number orderItemCount = (Number) entityManager.createNativeQuery(
                        "select count(*) from order_items where product_id = :productId")
                .setParameter("productId", productId)
                .getSingleResult();

        if (orderItemCount.longValue() > 0) {
            throw new IllegalStateException(
                    "Khong the xoa san pham da phat sinh don hang. Hay giu san pham de bao toan lich su don hang."
            );
        }

        entityManager.createNativeQuery("delete from cart_items where product_id = :productId")
                .setParameter("productId", productId)
                .executeUpdate();

        productRepository.delete(product);
        productRepository.flush();
        productSearchService.syncProductsToElasticsearch();
    }

    public AdminProductVariantDto createVariant(Long productId, CreateVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductVariant saved = saveNewVariant(product, request);
        productSearchService.syncProductsToElasticsearch();
        return convertVariantToDto(saved);
    }

    public AdminProductVariantDto updateVariant(
            Long variantId,
            UpdateVariantRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Variant request is required"
            );
        }

        ProductVariant variant =
                productVariantRepository.findById(variantId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Variant not found"
                                )
                        );

        if (request.getStockQty() != null) {

            if (request.getStockQty() < 0) {
                throw new IllegalArgumentException(
                        "Số lượng tồn kho không được âm"
                );
            }

            variant.setStockQty(
                    request.getStockQty()
            );
        }

        if (request.getPriceAdjustment() != null) {

            if (
                    request.getPriceAdjustment()
                            .signum() < 0
            ) {
                throw new IllegalArgumentException(
                        "Giá cộng thêm không được âm"
                );
            }

            variant.setPriceAdjustment(
                    request.getPriceAdjustment()
            );
        }

        if (request.getIsActive() != null) {
            variant.setIsActive(
                    request.getIsActive()
            );
        }

        ProductVariant updated =
                productVariantRepository.save(variant);

        productSearchService
                .syncProductsToElasticsearch();

        return convertVariantToDto(updated);
    }

    public void deleteVariant(Long variantId) {

        ProductVariant variant =
                productVariantRepository.findById(variantId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Variant not found"
                                )
                        );

        Product product = variant.getProduct();

        List<ProductVariant> variants =
                productVariantRepository.findByProduct(product);

        if (variants.size() <= 1) {
            throw new IllegalStateException(
                    "Sản phẩm phải có ít nhất một SKU. "
                            + "Không thể xóa SKU cuối cùng."
            );
        }

        productVariantRepository.delete(variant);

        productSearchService
                .syncProductsToElasticsearch();
    }

    public List<AdminProductVariantDto> getProductVariants(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        return productVariantRepository.findByProduct(product).stream()
                .map(this::convertVariantToDto)
                .collect(Collectors.toList());
    }

    private AdminProductDto convertToAdminDto(Product product) {
        List<ProductVariant> variants = productVariantRepository.findByProduct(product);
        Long totalStock = variants.stream()
                .mapToLong(v -> v.getStockQty() != null ? v.getStockQty() : 0L)
                .sum();

        List<ProductImage> images = productImageRepository.findByProduct(product);
        List<ProductSizeChart> sizeCharts = productSizeChartRepository.findByProductOrderByIdAsc(product);

        return AdminProductDto.builder()
                .id(product.getId())
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .salePrice(product.getSalePrice())
                .material(product.getMaterial())
                .fit(product.getFit())
                .gender(product.getGender())
                .status(product.getStatus())
                .isFeatured(product.getIsFeatured())
                .isNew(product.getIsNew())
                .totalStock(totalStock)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .variants(variants.stream().map(this::convertVariantToDto).collect(Collectors.toList()))
                .images(images.stream()
                        .map(img -> ProductImageDto.builder()
                        .id(img.getId())
                        .variantId(img.getVariant() != null ? img.getVariant().getId() : null)
                        .imageUrl(img.getImageUrl())
                        .altText(img.getAltText())
                        .isPrimary(img.getIsPrimary())
                        .displayOrder(img.getDisplayOrder())
                        .createdAt(img.getCreatedAt())
                        .build())
                        .collect(Collectors.toList()))
                .sizeCharts(sizeCharts.stream()
                        .map(chart -> ProductSizeChartDto.builder()
                                .id(chart.getId())
                                .sizeLabel(chart.getSizeLabel())
                                .minChest(chart.getMinChest())
                                .maxChest(chart.getMaxChest())
                                .minWaist(chart.getMinWaist())
                                .maxWaist(chart.getMaxWaist())
                                .minHip(chart.getMinHip())
                                .maxHip(chart.getMaxHip())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private AdminProductVariantDto convertVariantToDto(ProductVariant variant) {
        return AdminProductVariantDto.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .color(variant.getColor())
                .size(variant.getSize())
                .stockQty(variant.getStockQty())
                .priceAdjustment(variant.getPriceAdjustment())
                .isActive(variant.getIsActive())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }

    private void replaceProductImages(Product product, List<ProductImageDto> images) {
        productImageRepository.deleteAll(productImageRepository.findByProduct(product));

        if (images == null || images.isEmpty()) {
            return;
        }

        List<ProductImage> nextImages = images.stream()
                .filter(image -> image != null
                        && image.getImageUrl() != null
                        && !image.getImageUrl().isBlank())
                .map(image -> {
                    String imageUrl = image.getImageUrl().trim();

                    // DB: product_images.image_url VARCHAR(500)
                    if (imageUrl.length() > 500) {
                        throw new IllegalArgumentException(
                                "URL ảnh quá dài. imageUrl chỉ được tối đa 500 ký tự. "
                                        + "Không được gửi ảnh dạng base64/data:image."
                        );
                    }

                    String altText = image.getAltText();
                    if (altText == null || altText.isBlank()) {
                        altText = product.getName();
                    } else {
                        altText = altText.trim();
                    }

                    // DB: product_images.alt_text VARCHAR(200)
                    if (altText != null && altText.length() > 200) {
                        altText = altText.substring(0, 200);
                    }

                    return ProductImage.builder()
                            .product(product)
                            .variant(null)
                            .imageUrl(imageUrl)
                            .altText(altText)
                            .isPrimary(Boolean.TRUE.equals(image.getIsPrimary()))
                            .displayOrder(image.getDisplayOrder() == null ? 0 : image.getDisplayOrder())
                            .build();
                })
                .collect(Collectors.toList());

        if (!nextImages.isEmpty()
                && nextImages.stream().noneMatch(image -> Boolean.TRUE.equals(image.getIsPrimary()))) {
            nextImages.get(0).setIsPrimary(true);
        }

        productImageRepository.saveAll(nextImages);
    }

    private void replaceProductSizeCharts(Product product, List<ProductSizeChartDto> sizeCharts) {
        productSizeChartRepository.deleteByProduct(product);

        if (sizeCharts == null || sizeCharts.isEmpty()) {
            return;
        }

        List<ProductSizeChart> nextCharts = sizeCharts.stream()
                .filter(chart -> chart != null
                        && chart.getSizeLabel() != null
                        && !chart.getSizeLabel().isBlank())
                .map(chart -> {
                    validateSizeChart(chart);
                    return ProductSizeChart.builder()
                            .product(product)
                            .sizeLabel(chart.getSizeLabel().trim().toUpperCase())
                            .minChest(chart.getMinChest())
                            .maxChest(chart.getMaxChest())
                            .minWaist(chart.getMinWaist())
                            .maxWaist(chart.getMaxWaist())
                            .minHip(chart.getMinHip())
                            .maxHip(chart.getMaxHip())
                            .build();
                })
                .collect(Collectors.toList());

        productSizeChartRepository.saveAll(nextCharts);
    }

    private void validateSizeChart(ProductSizeChartDto chart) {
        if (chart.getSizeLabel() == null || chart.getSizeLabel().isBlank()) {
            throw new IllegalArgumentException("Size label is required");
        }

        if (chart.getSizeLabel().trim().length() > 20) {
            throw new IllegalArgumentException("Size label must be at most 20 characters");
        }

        validateRange("Chest", chart.getMinChest(), chart.getMaxChest());
        validateRange("Waist", chart.getMinWaist(), chart.getMaxWaist());
        validateRange("Hip", chart.getMinHip(), chart.getMaxHip());
    }

    private void validateRange(String label, Double min, Double max) {
        if (min == null || max == null || min <= 0 || max <= 0 || min > max) {
            throw new IllegalArgumentException(label + " size range is invalid");
        }
    }

    private String slugify(String name) {
        String slug = Normalizer.normalize(name == null ? "san-pham" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "san-pham" : slug;
    }

    private String uniqueSlug(String name, Long currentProductId) {
        String baseSlug = slugify(name);

        // DB: products.slug VARCHAR(200)
        if (baseSlug.length() > 200) {
            baseSlug = baseSlug.substring(0, 200);
            baseSlug = baseSlug.replaceAll("-+$", "");
        }

        String candidate = baseSlug;
        int suffix = 2;

        while (productRepository.findBySlug(candidate)
                .filter(product -> currentProductId == null
                        || !product.getId().equals(currentProductId))
                .isPresent()) {

            String suffixText = "-" + suffix;
            int maxBaseLength = 200 - suffixText.length();

            String shortenedBase = baseSlug.length() > maxBaseLength
                    ? baseSlug.substring(0, maxBaseLength)
                    : baseSlug;

            shortenedBase = shortenedBase.replaceAll("-+$", "");
            candidate = shortenedBase + suffixText;
            suffix++;
        }

        return candidate;
    }

    private String generateSku(Long productId, String color, String size) {
        String normalizedColor = normalizeSkuPart(color);
        String normalizedSize = normalizeSkuPart(size);

        if ("DEFAULT".equals(normalizedColor) && "DEFAULT".equals(normalizedSize)) {
            return String.format("SKU-%d-DEFAULT", productId);
        }

        return String.format("SKU-%d-%s-%s", productId, normalizedColor, normalizedSize);
    }

    private String normalizeSkuPart(String value) {
        String normalized = Normalizer.normalize(value == null ? "DEFAULT" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .trim()
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        return normalized.isBlank() ? "DEFAULT" : normalized;
    }

    private String uniqueSku(String baseSku) {
        String candidate = baseSku;
        int suffix = 2;

        while (productVariantRepository.findBySku(candidate).isPresent()) {
            candidate = baseSku + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    private void createInitialVariants(
            Product product,
            CreateProductRequest request
    ) {

        List<CreateVariantRequest> requestedVariants =
                request.getVariants() == null
                        ? List.of()
                        : request.getVariants()
                                .stream()
                                .filter(variant ->
                                        variant != null
                                                && variant.getColor() != null
                                                && !variant.getColor().isBlank()
                                                && variant.getSize() != null
                                                && !variant.getSize().isBlank()
                                )
                                .toList();

        /*
         * TRƯỜNG HỢP 1:
         * Sản phẩm không có biến thể.
         *
         * Ví dụ:
         * Túi Tote
         * stockQty = 50
         *
         * Backend tự tạo:
         * SKU-15-DEFAULT
         */
        if (requestedVariants.isEmpty()) {

            Integer stockQty =
                    request.getStockQty() == null
                            ? 0
                            : request.getStockQty();

            CreateVariantRequest defaultVariant =
                    CreateVariantRequest.builder()
                            .color("DEFAULT")
                            .size("DEFAULT")
                            .stockQty(stockQty)
                            .priceAdjustment(java.math.BigDecimal.ZERO)
                            .build();

            saveNewVariant(product, defaultVariant);

            return;
        }

        /*
         * TRƯỜNG HỢP 2:
         * Sản phẩm có nhiều biến thể.
         */
        requestedVariants.forEach(
                variant -> saveNewVariant(product, variant)
        );
    }

    private ProductVariant saveNewVariant(
            Product product,
            CreateVariantRequest request
    ) {

        validateVariantRequest(request);

        String color = request.getColor().trim();
        String size = request.getSize().trim();

        // Không cho cùng một sản phẩm có hai biến thể giống hệt nhau
        boolean duplicated =
                productVariantRepository
                        .existsByProductAndColorIgnoreCaseAndSizeIgnoreCase(
                                product,
                                color,
                                size
                        );

        if (duplicated) {
            throw new IllegalArgumentException(
                    "Biến thể " + color + " / " + size + " đã tồn tại"
            );
        }

        String sku = uniqueSku(
                generateSku(
                        product.getId(),
                        color,
                        size
                )
        );

        ProductVariant variant =
                ProductVariant.builder()
                        .product(product)
                        .sku(sku)
                        .color(color)
                        .size(size)
                        .stockQty(request.getStockQty())
                        .priceAdjustment(
                                request.getPriceAdjustment() == null
                                        ? java.math.BigDecimal.ZERO
                                        : request.getPriceAdjustment()
                        )
                        .isActive(true)
                        .build();

        return productVariantRepository.save(variant);
    }

    private void validateVariantRequest(
            CreateVariantRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Thông tin biến thể không được để trống"
            );
        }

        if (
                request.getColor() == null
                        || request.getColor().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Màu sắc không hợp lệ"
            );
        }

        if (
                request.getSize() == null
                        || request.getSize().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Kích thước không hợp lệ"
            );
        }

        if (
                request.getStockQty() == null
                        || request.getStockQty() < 0
        ) {
            throw new IllegalArgumentException(
                    "Số lượng tồn kho phải lớn hơn hoặc bằng 0"
            );
        }

        if (
                request.getPriceAdjustment() != null
                        && request.getPriceAdjustment().signum() < 0
        ) {
            throw new IllegalArgumentException(
                    "Giá cộng thêm không được âm"
            );
        }
    }

    private void validateProductRequest(CreateProductRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Product request is required");
        }

        if (request.getBrandId() == null || request.getBrandId() <= 0) {
            throw new IllegalArgumentException("Brand is required");
        }

        if (request.getCategoryId() == null || request.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Category is required");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }

        // DB: products.name VARCHAR(200)
        if (request.getName().trim().length() > 200) {
            throw new IllegalArgumentException("Tên sản phẩm chỉ được tối đa 200 ký tự");
        }

        if (request.getBasePrice() == null || request.getBasePrice().signum() <= 0) {
            throw new IllegalArgumentException("Base price must be greater than 0");
        }

        if (
                request.getVariants() == null
                        || request.getVariants().isEmpty()
        ) {

            // Sản phẩm không có biến thể
            if (
                    request.getStockQty() == null
                            || request.getStockQty() < 0
            ) {
                throw new IllegalArgumentException(
                        "Số lượng tồn kho phải lớn hơn hoặc bằng 0"
                );
            }

        } else {

            // Sản phẩm có biến thể
            request.getVariants()
                    .forEach(this::validateVariantRequest);
        }

        if (request.getSalePrice() != null && request.getSalePrice().signum() < 0) {
            throw new IllegalArgumentException("Sale price không được âm");
        }

        if (request.getMaterial() != null && request.getMaterial().length() > 150) {
            throw new IllegalArgumentException("Material chỉ được tối đa 150 ký tự");
        }

        if (request.getFit() != null && request.getFit().length() > 100) {
            throw new IllegalArgumentException("Fit chỉ được tối đa 100 ký tự");
        }

        if (request.getGender() != null && request.getGender().length() > 30) {
            throw new IllegalArgumentException("Gender chỉ được tối đa 30 ký tự");
        }

        if (request.getStatus() != null && request.getStatus().length() > 30) {
            throw new IllegalArgumentException("Status chỉ được tối đa 30 ký tự");
        }

        if (request.getImages() != null) {
            for (ProductImageDto image : request.getImages()) {
                if (image == null || image.getImageUrl() == null || image.getImageUrl().isBlank()) {
                    continue;
                }

                String imageUrl = image.getImageUrl().trim();

                if (imageUrl.length() > 500) {
                    throw new IllegalArgumentException(
                            "URL ảnh chỉ được tối đa 500 ký tự. "
                                    + "Nếu đang gửi base64/data:image, hãy đổi sang URL ảnh."
                    );
                }
            }
        }
    }

}
