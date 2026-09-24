package com.fashion.product.config;

import com.fashion.brand.entity.Brand;
import com.fashion.brand.repository.BrandRepository;
import com.fashion.category.entity.Category;
import com.fashion.category.repository.CategoryRepository;
import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductImage;
import com.fashion.product.entity.ProductVariant;
import com.fashion.product.repository.ProductImageRepository;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
@Order(2)
@RequiredArgsConstructor
public class CatalogInitializer implements CommandLineRunner {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Brand urbanVogue = ensureBrand(
                "Urban Vogue",
                "urban-vogue",
                "Streetwear hiện đại, dễ mặc hằng ngày.",
                "https://images.unsplash.com/photo-1775704847874-1f9f403e4edb?auto=format&fit=crop&w=240&q=80");
        Brand monarchStudio = ensureBrand(
                "Monarch Studio",
                "monarch-studio",
                "Những item polished cho công sở và cuối tuần.",
                "https://images.unsplash.com/photo-1684974833710-dbabcbf884df?auto=format&fit=crop&w=240&q=80");
        Brand northPeak = ensureBrand(
                "North Peak",
                "north-peak",
                "Outerwear nhẹ, bền và năng động.",
                "https://images.unsplash.com/photo-1773145297071-3cff5c9d143e?auto=format&fit=crop&w=240&q=80");
        Brand atelierMuse = ensureBrand(
                "Atelier Muse",
                "atelier-muse",
                "Đầm và phụ kiện tối giản, thanh lịch.",
                "https://images.unsplash.com/photo-1577660002965-04865592fc60?auto=format&fit=crop&w=240&q=80");

        Category tShirts = ensureCategory("Áo thun", "ao-thun", "Áo thun cotton, knit và jersey cho outfit hằng ngày.");
        Category pants = ensureCategory("Quần", "quan", "Jeans, trousers và quần ống rộng dễ phối đồ.");
        Category jackets = ensureCategory("Áo khoác", "ao-khoac", "Bomber, denim jacket và outerwear nhẹ.");
        Category dresses = ensureCategory("Đầm", "dam", "Đầm midi, slip dress và các kiểu đầm thanh lịch.");
        Category accessories = ensureCategory("Phụ kiện", "phu-kien", "Túi, sneaker và phụ kiện hoàn thiện outfit.");

        seedProduct(
                urbanVogue,
                tShirts,
                "Áo thun Essential Cotton",
                "ao-thun-basic",
                "Cotton tee mềm, thoáng và dễ phối.",
                "Áo thun cotton cao cấp với form regular gọn gàng, phù hợp đi học, đi làm và xuống phố.",
                BigDecimal.valueOf(320000),
                BigDecimal.valueOf(280000),
                "Cotton 100%",
                "Regular",
                "UNISEX",
                true,
                true,
                List.of(
                        variant("ATB-001-BLK-S", "Black", "S", 24, 0),
                        variant("ATB-001-BLK-M", "Black", "M", 28, 0),
                        variant("ATB-001-WHT-M", "White", "M", 22, 0)),
                List.of(
                        image("ATB-001-WHT-M", "https://images.unsplash.com/photo-1577660002965-04865592fc60?auto=format&fit=crop&w=1200&q=85", "Áo thun cotton trắng với denim jacket", true, 1),
                        image("ATB-001-BLK-M", "https://images.unsplash.com/photo-1775704847874-1f9f403e4edb?auto=format&fit=crop&w=1200&q=85", "Áo thun basic phối cùng jeans", false, 2)));

        seedProduct(
                monarchStudio,
                pants,
                "Quần jean Relaxed Blue",
                "quan-jean-relaxed",
                "Denim relaxed, ống đứng vừa phải.",
                "Quần jean denim xanh form relaxed, co giãn nhẹ, giữ form đẹp cho outfit hằng ngày.",
                BigDecimal.valueOf(620000),
                BigDecimal.valueOf(560000),
                "Denim cotton blend",
                "Relaxed",
                "UNISEX",
                true,
                false,
                List.of(
                        variant("QJR-001-BLU-28", "Blue", "28", 18, 0),
                        variant("QJR-001-BLU-30", "Blue", "30", 20, 0),
                        variant("QJR-001-BLU-32", "Blue", "32", 16, 0)),
                List.of(image("QJR-001-BLU-30", "https://images.unsplash.com/photo-1684974833710-dbabcbf884df?auto=format&fit=crop&w=1200&q=85", "Quần jean relaxed trong outfit denim", true, 1)));

        seedProduct(
                northPeak,
                jackets,
                "Áo khoác Bomber Urban",
                "ao-khoac-bomber",
                "Bomber nhẹ cho ngày se lạnh.",
                "Áo khoác bomber chất nylon mềm, lót thoáng, hợp phong cách streetwear hiện đại.",
                BigDecimal.valueOf(780000),
                BigDecimal.valueOf(690000),
                "Nylon twill",
                "Regular",
                "UNISEX",
                true,
                true,
                List.of(
                        variant("AKB-001-BLK-M", "Black", "M", 14, 0),
                        variant("AKB-001-BLK-L", "Black", "L", 12, 0),
                        variant("AKB-001-OLV-M", "Olive", "M", 10, 20000)),
                List.of(image("AKB-001-BLK-M", "https://images.unsplash.com/photo-1773145297071-3cff5c9d143e?auto=format&fit=crop&w=1200&q=85", "Áo khoác bomber đen chụp ngoài trời", true, 1)));

        seedProduct(
                atelierMuse,
                dresses,
                "Đầm Midi Linen Sand",
                "dam-midi-linen",
                "Đầm midi linen thanh lịch.",
                "Đầm midi linen pha cotton, dáng A-line nhẹ nhàng cho công sở và cafe cuối tuần.",
                BigDecimal.valueOf(760000),
                BigDecimal.valueOf(670000),
                "Linen cotton",
                "A-line",
                "FEMALE",
                true,
                true,
                List.of(
                        variant("DML-001-SND-S", "Sand", "S", 11, 0),
                        variant("DML-001-SND-M", "Sand", "M", 13, 0)),
                List.of(image("DML-001-SND-M", "https://images.unsplash.com/photo-1684974833710-dbabcbf884df?auto=format&fit=crop&w=1200&q=85", "Đầm midi linen phối cùng jacket denim", true, 1)));

        seedProduct(
                urbanVogue,
                jackets,
                "Áo khoác Denim Lightwash",
                "ao-khoac-denim-lightwash",
                "Denim jacket xanh sáng, dễ layer.",
                "Áo khoác denim wash sáng, chất đứng form, hợp với áo thun và đầm basic.",
                BigDecimal.valueOf(860000),
                BigDecimal.valueOf(749000),
                "Denim 12oz",
                "Boxy",
                "UNISEX",
                false,
                true,
                List.of(
                        variant("ADL-001-BLU-S", "Light Blue", "S", 9, 0),
                        variant("ADL-001-BLU-M", "Light Blue", "M", 12, 0)),
                List.of(image("ADL-001-BLU-M", "https://images.unsplash.com/photo-1577660002965-04865592fc60?auto=format&fit=crop&w=1200&q=85", "Áo khoác denim wash sáng", true, 1)));

        seedProduct(
                monarchStudio,
                tShirts,
                "Áo knit Tank Ivory",
                "ao-knit-tank-ivory",
                "Tank top knit ivory tối giản.",
                "Áo tank knit cổ tròn, bề mặt mịn, mặc riêng hoặc layer với blazer và cardigan.",
                BigDecimal.valueOf(390000),
                BigDecimal.valueOf(339000),
                "Rib knit",
                "Slim",
                "FEMALE",
                false,
                true,
                List.of(
                        variant("AKT-001-IVR-S", "Ivory", "S", 15, 0),
                        variant("AKT-001-IVR-M", "Ivory", "M", 17, 0)),
                List.of(image("AKT-001-IVR-M", "https://images.unsplash.com/photo-1775704847874-1f9f403e4edb?auto=format&fit=crop&w=1200&q=85", "Áo tank ivory phối jeans", true, 1)));

        seedProduct(
                atelierMuse,
                accessories,
                "Túi Mini Crescent",
                "tui-mini-crescent",
                "Túi đeo vai nhỏ gọn, sáng outfit.",
                "Túi mini dáng crescent với quai đeo vai, phù hợp outfit tối giản và đi chơi.",
                BigDecimal.valueOf(520000),
                BigDecimal.valueOf(459000),
                "Vegan leather",
                "One size",
                "FEMALE",
                true,
                false,
                List.of(
                        variant("TMC-001-BLK-OS", "Black", "OS", 20, 0),
                        variant("TMC-001-TAN-OS", "Tan", "OS", 15, 0)),
                List.of(image("TMC-001-BLK-OS", "https://images.unsplash.com/photo-1559563458-527698bf5295?auto=format&fit=crop&w=1200&q=85", "Túi mini crescent trong boutique", true, 1)));

        seedProduct(
                northPeak,
                accessories,
                "Sneaker Cloud Runner",
                "sneaker-cloud-runner",
                "Sneaker êm chân, dễ đi cả ngày.",
                "Sneaker đệm nhẹ với form clean, dễ phối cùng jeans, shorts và set casual.",
                BigDecimal.valueOf(920000),
                BigDecimal.valueOf(829000),
                "Mesh, rubber",
                "Comfort",
                "UNISEX",
                false,
                true,
                List.of(
                        variant("SCR-001-GRY-39", "Grey", "39", 8, 0),
                        variant("SCR-001-GRY-40", "Grey", "40", 9, 0),
                        variant("SCR-001-GRY-41", "Grey", "41", 7, 0)),
                List.of(image("SCR-001-GRY-40", "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=1200&q=85", "Sneaker xám chụp sản phẩm", true, 1)));
    }

    private Brand ensureBrand(String name, String slug, String description, String logoUrl) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseGet(() -> Brand.builder().slug(slug).build());
        brand.setName(name);
        brand.setDescription(description);
        brand.setLogoUrl(logoUrl);
        brand.setIsActive(true);
        return brandRepository.save(brand);
    }

    private Category ensureCategory(String name, String slug, String description) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseGet(() -> Category.builder().slug(slug).build());
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        category.setIsActive(true);
        return categoryRepository.save(category);
    }

    private void seedProduct(
            Brand brand,
            Category category,
            String name,
            String slug,
            String shortDescription,
            String description,
            BigDecimal basePrice,
            BigDecimal salePrice,
            String material,
            String fit,
            String gender,
            boolean featured,
            boolean isNew,
            List<VariantSeed> variants,
            List<ImageSeed> images) {
        Product product = productRepository.findBySlug(slug)
                .orElseGet(() -> Product.builder().slug(slug).build());
        product.setBrand(brand);
        product.setCategory(category);
        product.setName(name);
        product.setShortDescription(shortDescription);
        product.setDescription(description);
        product.setBasePrice(basePrice);
        product.setSalePrice(salePrice);
        product.setMaterial(material);
        product.setFit(fit);
        product.setGender(gender);
        product.setStatus("ACTIVE");
        product.setIsFeatured(featured);
        product.setIsNew(isNew);
        Product savedProduct = productRepository.save(product);

        for (VariantSeed seed : variants) {
            ProductVariant productVariant = productVariantRepository.findBySku(seed.sku())
                    .or(() -> productVariantRepository.findByProductAndColorIgnoreCaseAndSizeIgnoreCase(
                            savedProduct, seed.color(), seed.size()))
                    .orElseGet(() -> ProductVariant.builder().sku(seed.sku()).build());
            productVariant.setProduct(savedProduct);
            productVariant.setSku(seed.sku());
            productVariant.setColor(seed.color());
            productVariant.setSize(seed.size());
            productVariant.setStockQty(seed.stockQty());
            productVariant.setPriceAdjustment(BigDecimal.valueOf(seed.priceAdjustment()));
            productVariant.setIsActive(true);
            productVariantRepository.save(productVariant);
        }

        productImageRepository.deleteAll(productImageRepository.findByProduct(savedProduct));
        for (ImageSeed image : images) {
            ProductImage productImage = ProductImage.builder()
                    .product(savedProduct)
                    .variant(productVariantRepository.findBySku(image.variantSku()).orElse(null))
                    .imageUrl(image.url())
                    .altText(image.altText())
                    .isPrimary(image.primary())
                    .displayOrder(image.displayOrder())
                    .build();
            productImageRepository.save(productImage);
        }
    }

    private VariantSeed variant(String sku, String color, String size, Integer stockQty, Integer priceAdjustment) {
        return new VariantSeed(sku, color, size, stockQty, priceAdjustment);
    }

    private ImageSeed image(String variantSku, String url, String altText, boolean primary, Integer displayOrder) {
        return new ImageSeed(variantSku, url, altText, primary, displayOrder);
    }

    private record VariantSeed(String sku, String color, String size, Integer stockQty, Integer priceAdjustment) {
    }

    private record ImageSeed(String variantSku, String url, String altText, boolean primary, Integer displayOrder) {
    }
}
