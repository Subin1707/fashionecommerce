package com.fashion.smartsize;

import com.fashion.brand.entity.Brand;
import com.fashion.brand.service.BrandService;
import com.fashion.category.entity.Category;
import com.fashion.category.service.CategoryService;
import com.fashion.product.entity.Product;
import com.fashion.product.repository.ProductRepository;
import com.fashion.smartsize.dto.SmartSizeRequest;
import com.fashion.smartsize.dto.SmartSizeResult;
import com.fashion.smartsize.service.SmartSizeService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SmartSizeServiceTest {

    @Autowired
    private SmartSizeService smartSizeService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Test
    void shouldRecommendSizeForBasicTShirt() {
        Product product = createProduct("basic-t-shirt");

        SmartSizeResult result = smartSizeService.recommendSize(product.getId(),
                request(168.0, 58.0, 94.0, 76.0, 98.0));

        assertThat(result.getRecommendedSize()).isEqualTo("M");
        assertThat(result.getConfidence()).endsWith("%");
        assertThat(result.getReasoning()).contains("Goi y size M");
    }

    @Test
    void shouldRecommendSmallForSmallMeasurements() {
        Product product = createProduct("smart-size-small");

        SmartSizeResult result = smartSizeService.recommendSize(product.getId(),
                request(160.0, 50.0, 88.0, 70.0, 92.0));

        assertThat(result.getRecommendedSize()).isEqualTo("S");
    }

    @Test
    void shouldRecommendMediumForMediumMeasurements() {
        Product product = createProduct("smart-size-medium");

        SmartSizeResult result = smartSizeService.recommendSize(product.getId(),
                request(168.0, 60.0, 94.0, 76.0, 98.0));

        assertThat(result.getRecommendedSize()).isEqualTo("M");
    }

    @Test
    void shouldRecommendLargeForLargeMeasurements() {
        Product product = createProduct("smart-size-large");

        SmartSizeResult result = smartSizeService.recommendSize(product.getId(),
                request(175.0, 70.0, 100.0, 84.0, 104.0));

        assertThat(result.getRecommendedSize()).isEqualTo("L");
    }

    @Test
    void shouldNotRecommendSmallOrMediumForLargeMeasurements() {
        Product product = createProduct("smart-size-xl-regression");

        SmartSizeResult result = smartSizeService.recommendSize(product.getId(),
                request(170.0, 80.0, 105.0, 94.0, 106.0));

        assertThat(result.getRecommendedSize()).isEqualTo("XL");
        assertThat(result.getRecommendedSize()).isNotIn("S", "M");
    }

    @Test
    void shouldRecommendDoubleExtraLargeForVeryLargeMeasurements() {
        Product product = createProduct("smart-size-xxl");

        SmartSizeResult result = smartSizeService.recommendSize(product.getId(),
                request(180.0, 90.0, 114.0, 102.0, 120.0));

        assertThat(result.getRecommendedSize()).isEqualTo("XXL");
    }

    private SmartSizeRequest request(double height, double weight, double chest, double waist, double hip) {
        SmartSizeRequest request = new SmartSizeRequest();
        request.setHeight(height);
        request.setWeight(weight);
        request.setChest(chest);
        request.setWaist(waist);
        request.setHip(hip);
        return request;
    }

    private Product createProduct(String slugPrefix) {
        String suffix = UUID.randomUUID().toString();
        Brand brand = brandService.createBrand(Brand.builder()
                .name("SmartSizeTestBrand-" + suffix)
                .slug("smartsize-test-brand-" + suffix)
                .description("Brand for smartsize test")
                .build());
        Category category = categoryService.createCategory(Category.builder()
                .name("SmartSizeTestCategory-" + suffix)
                .slug("smartsize-test-category-" + suffix)
                .description("Category for smartsize test")
                .build());

        return productRepository.save(Product.builder()
                .name("Smart Size Product " + suffix)
                .slug(slugPrefix + "-" + suffix)
                .description("Cotton casual")
                .basePrice(BigDecimal.valueOf(200000.0))
                .salePrice(BigDecimal.valueOf(180000.0))
                .category(category)
                .brand(brand)
                .material("Cotton")
                .status("ACTIVE")
                .build());
    }
}
