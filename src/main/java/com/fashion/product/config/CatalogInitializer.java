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
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
@Order(2)
public class CatalogInitializer implements CommandLineRunner {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Brand zumi = ensureBrand("ZUMI", "zumi", "Thương hiệu thời trang Việt với sản phẩm cotton compact sản xuất trong nước.", null);
        Brand mazzola = ensureBrand("Mazzola", "mazzola", "Thương hiệu Việt chuyên denim với phom dáng hiện đại và chất liệu bền.", null);
        Brand heboz = ensureBrand("HEBOZ", "heboz", "Thời trang nam Việt Nam với các thiết kế ứng dụng cho đời sống đô thị.", null);
        Brand hity = ensureBrand("Hity", "hity", "Thương hiệu thiết kế Việt chú trọng chất liệu tự nhiên và phom dáng thanh lịch.", null);
        Brand h2t = ensureBrand("H2T Store", "h2t-store", "Thời trang trẻ Việt Nam, tập trung vào sản phẩm denim và trang phục thường ngày.", null);
        Brand gianSaigon = ensureBrand("GIAN Sài Gòn", "gian-sai-gon", "Local brand Sài Gòn với các thiết kế dệt gân và phom dáng tối giản.", null);
        Brand vascara = ensureBrand("Vascara", "vascara", "Thương hiệu phụ kiện Việt Nam với túi xách và giày dép cho nữ.", null);
        Brand bitis = ensureBrand("Biti's Hunter", "bitis-hunter", "Giày thể thao Việt Nam thiết kế cho di chuyển và sử dụng hằng ngày.", null);
        Brand rafoz = ensureBrand("RAFOZ", "rafoz", "Local brand Việt với trang phục pique và polo nam.", null);
        Brand lethnic = ensureBrand("LETHNIC", "lethnic", "Đồ da thủ công Việt Nam, thiết kế tối giản từ da bò thật.", null);

        Category tShirts = ensureCategory("Áo thun", "ao-thun", "Áo thun cotton, knit và jersey cho outfit hằng ngày.");
        Category pants = ensureCategory("Quần", "quan", "Jeans, trousers và quần ống rộng dễ phối đồ.");
        Category jackets = ensureCategory("Áo khoác", "ao-khoac", "Bomber, denim jacket và outerwear nhẹ.");
        Category dresses = ensureCategory("Đầm", "dam", "Đầm midi, slip dress và các kiểu đầm thanh lịch.");
        Category accessories = ensureCategory("Phụ kiện", "phu-kien", "Túi, sneaker và phụ kiện hoàn thiện outfit.");

        seedProduct(
                zumi,
                tShirts,
                "Áo thun cotton compact Sài Gòn",
                "ao-thun-basic",
                "Áo thun cổ tròn từ cotton compact, phom regular và bề mặt vải mịn.",
                "Áo thun ZUMI sản xuất tại Việt Nam từ cotton compact 100%, bề mặt mịn, thoáng và giữ phom sau nhiều lần giặt. Cổ bo gọn, tay ngắn và thân áo regular dễ phối với denim hoặc quần kaki. Ảnh chụp riêng sản phẩm trên nền sáng, không dùng ảnh người mẫu.",
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
                        variant("ATB-001-BLK-L", "Black", "L", 22, 0)),
                List.of(image("ATB-001-BLK-M", "https://zumifashion.com/Uploads/images/product/item/ao-thun-cotton-100-compact-mau-den/f0c66747-48d0-42df-a25c-efa050c5eed4.jpg", "Áo thun cotton compact Sài Gòn màu đen, ảnh sản phẩm", true, 1)));

        seedProduct(
                mazzola,
                pants,
                "Quần jean nam xanh đậm Mazzola",
                "quan-jean-relaxed",
                "Quần jean denim xanh đậm, phom relaxed và thiết kế năm túi cổ điển.",
                "Quần jean Mazzola màu xanh chàm đậm với phom relaxed, cạp vừa và ống đứng thoải mái. Denim cotton bền, đường may tương phản và thiết kế năm túi tiện dụng. Có các cỡ eo 28, 30 và 32; phù hợp mặc hằng ngày cùng áo thun hoặc sơ mi.",
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
                List.of(image("QJR-001-BLU-30", "https://cdn.hstatic.net/products/1000133495/4_8c237b78d8dd4946a6168c251c26f64c.jpg", "Quần jean nam xanh đậm Mazzola, ảnh sản phẩm", true, 1)));

        seedProduct(
                heboz,
                jackets,
                "Áo khoác bomber gió đen HEBOZ",
                "ao-khoac-bomber",
                "Bomber vải gió màu đen, cổ bo và túi khóa kéo ở tay.",
                "Áo khoác bomber HEBOZ dùng vải gió nhẹ, khóa kéo thân trước, cổ tay và gấu bo rib. Túi khóa kéo ở tay áo giúp cất đồ nhỏ gọn; lớp trong thoáng, thích hợp khoác ngoài khi đi lại hoặc trong thời tiết se mát. Ảnh chụp riêng sản phẩm, thể hiện rõ phom áo và chi tiết khóa.",
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
                List.of(image("AKB-001-BLK-M", "https://cdn.hstatic.net/products/1000333546/_o_kho_c_heboz_bomber_gi__3m_-_0003026__3__10f40a853cbe47d3a24d5f77ef88b1a3_master.jpg", "Áo khoác bomber gió đen HEBOZ, ảnh sản phẩm", true, 1)));

        seedProduct(
                hity,
                dresses,
                "Đầm linen cổ tim dáng midi Hity",
                "dam-midi-linen",
                "Đầm linen màu cát, cổ tim và dáng midi chữ A.",
                "Đầm Hity may từ linen pha cotton, có bề mặt thoáng nhẹ và độ rủ tự nhiên. Cổ tim, tay ngắn và dáng midi chữ A tạo vẻ thanh lịch; hai cỡ S và M phù hợp mặc đi làm hoặc dạo phố. Giặt nhẹ và phơi trong bóng râm để giữ màu vải.",
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
                List.of(image("DML-001-SND-M", "https://product.hstatic.net/1000154340/product/vai_lanh_do_dam_co_tim_kieu_vintage_co_dien_thoi_trang_thiet_ke_hity_2_dd759789d7d4499c91a79600b78dfaac_master.jpg", "Đầm linen cổ tim dáng midi Hity, ảnh sản phẩm", true, 1)));

        seedProduct(
                h2t,
                jackets,
                "Áo khoác denim xanh sáng H2T",
                "ao-khoac-denim-lightwash",
                "Áo khoác denim xanh wash sáng, cổ bẻ và hai túi ngực có nắp.",
                "Áo khoác denim H2T dáng regular, chất vải đứng phom với đường chần nổi. Thiết kế có hàng nút kim loại, hai túi ngực nắp gài và túi bên hông; lớp wash xanh sáng dễ phối với áo thun. Có hai cỡ S và M.",
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
                List.of(image("ADL-001-BLU-M", "https://product.hstatic.net/200000201725/product/z5872947054935_d6ef51646576d3ce7b601aa75f51763e_1025ef55497d43c6b0ffb5131c93530c_master.jpg", "Áo khoác denim xanh sáng H2T, ảnh sản phẩm", true, 1)));

        seedProduct(
                gianSaigon,
                tShirts,
                "Áo tank dệt gân đỏ GIAN Sài Gòn",
                "ao-knit-tank-ivory",
                "Áo tank dệt gân màu đỏ, cổ tròn và dáng ôm gọn.",
                "Thiết kế GIAN Sài Gòn dùng vải dệt gân co giãn nhẹ, ôm vừa cơ thể mà vẫn thoải mái. Cổ tròn, nách khoét gọn và bề mặt gân dọc tạo điểm nhấn; có thể mặc riêng trong ngày nóng hoặc phối lớp cùng sơ mi và áo khoác mỏng.",
                BigDecimal.valueOf(390000),
                BigDecimal.valueOf(339000),
                "Rib knit",
                "Slim",
                "FEMALE",
                false,
                true,
                List.of(
                        variant("AKT-001-RED-S", "Red", "S", 15, 0),
                        variant("AKT-001-RED-M", "Red", "M", 17, 0)),
                List.of(image("AKT-001-RED-M", "https://product.hstatic.net/200000911315/product/5_8828200c57404a6591ae5db7f2619de7_master.png", "Áo tank dệt gân đỏ GIAN Sài Gòn, ảnh sản phẩm", true, 1)));

        seedProduct(
                vascara,
                accessories,
                "Túi đeo chéo da đen Vascara",
                "tui-mini-crescent",
                "Túi da màu đen nắp gập, khóa kim loại và dây đeo điều chỉnh.",
                "Túi Vascara dáng hộp nhỏ làm từ da, nắp gập có khóa xoay kim loại màu vàng. Ngăn chính rộng vừa điện thoại và vật dụng cá nhân, kèm ngăn phụ bên trong. Dây đeo điều chỉnh để dùng như túi vai hoặc túi đeo chéo; lau sạch bằng khăn mềm sau khi sử dụng.",
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
                List.of(image("TMC-001-BLK-OS", "https://www.vascara.com/uploads/cms_productmedia/2025/November/16/tui-deo-cheo-nap-gap-nhan-khoa-xoay-sho-0292-mau-den-0-765530-1763272361.jpg", "Túi đeo chéo da đen Vascara SHO 0292, ảnh sản phẩm", true, 1)));

        seedProduct(
                bitis,
                accessories,
                "Giày thể thao trắng Biti's Hunter",
                "sneaker-cloud-runner",
                "Giày thể thao trắng, thân lưới thoáng và đế nhẹ cho sử dụng hằng ngày.",
                "Biti's Hunter HSM009301TRG có thân giày trắng phối xám nhạt, lớp lưới thoáng khí và các mảng phủ tăng độ bền. Đế EVA nhẹ hỗ trợ giảm chấn khi đi bộ; cổ giày đệm êm và dây buộc giúp điều chỉnh độ ôm. Có các cỡ 39, 40 và 41.",
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
                List.of(image("SCR-001-GRY-40", "https://cdn.hstatic.net/products/1000230642/hsm009301trg__3__795141aaf6be4051a5243099c41a1148.png", "Giày thể thao trắng Biti's Hunter HSM009301TRG, ảnh sản phẩm", true, 1)));

        seedProduct(
                rafoz,
                tShirts,
                "Áo polo pique nam Sông Hồng",
                "ao-polo-pique-nam-song-hong",
                "Polo pique màu be, cổ bẻ và nẹp hai nút.",
                "Áo polo nam RAFOZ dùng vải pique dệt mắt nhỏ, thoáng khí và giữ bề mặt đứng phom. Cổ bẻ, nẹp hai nút và tay bo tạo vẻ gọn gàng cho cả ngày làm việc lẫn dịp cuối tuần. Mẫu màu be có các cỡ S, M và L; giặt mặt trái với màu tương tự để giữ màu vải.",
                BigDecimal.valueOf(480000),
                BigDecimal.valueOf(429000),
                "Cotton pique",
                "Regular",
                "MALE",
                true,
                true,
                List.of(
                        variant("APS-001-BGE-S", "Beige", "S", 12, 0),
                        variant("APS-001-BGE-M", "Beige", "M", 16, 0),
                        variant("APS-001-BGE-L", "Beige", "L", 10, 0)),
                List.of(image("APS-001-BGE-M", "https://product.hstatic.net/200000578961/product/00009672_89dda2a857204cdaac627b2dff4538be_master.jpg", "Áo polo pique nam RAFOZ màu be, ảnh sản phẩm", true, 1)));

        seedProduct(
                lethnic,
                accessories,
                "Ví gập da bò mềm thủ công",
                "vi-gap-da-bo-mem-thu-cong",
                "Ví gập da bò màu nâu, nhỏ gọn với nhiều ngăn thẻ.",
                "Ví gập LETHNIC làm từ da bò sáp mềm, hoàn thiện thủ công tại Việt Nam. Thiết kế gọn có tám khe thẻ và một ngăn tiền giấy dài; bề mặt da lên màu tự nhiên theo thời gian sử dụng. Kích thước vừa túi quần, phù hợp dùng hằng ngày; tránh ngâm nước và dưỡng da định kỳ bằng khăn mềm.",
                BigDecimal.valueOf(690000),
                BigDecimal.valueOf(620000),
                "Da bò sáp",
                "One size",
                "UNISEX",
                false,
                true,
                List.of(variant("VGT-001-TAN-OS", "Tan", "OS", 18, 0)),
                List.of(image("VGT-001-TAN-OS", "https://lethnic.vn/cdn/shop/products/Pasic_affc0d60-50c5-4d6c-a5df-95661d6ccf84_1200x1200.jpg?v=1619765600", "Ví gập da bò sáp LETHNIC màu nâu, ảnh sản phẩm", true, 1)));

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
        List<ImageSeed> usableImages = images == null ? List.of() : images.stream()
                .filter(image -> image != null && image.url() != null && !image.url().isBlank())
                .toList();
        for (ImageSeed image : usableImages) {
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

    public CatalogInitializer(final BrandRepository brandRepository, final CategoryRepository categoryRepository, final ProductRepository productRepository, final ProductVariantRepository productVariantRepository, final ProductImageRepository productImageRepository) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
    }
}
