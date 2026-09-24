package com.fashion.style;

import com.fashion.brand.entity.Brand;
import com.fashion.brand.service.BrandService;
import com.fashion.category.entity.Category;
import com.fashion.category.service.CategoryService;
import com.fashion.product.entity.Product;
import com.fashion.product.repository.ProductRepository;
import com.fashion.style.dto.StyleMatchRequest;
import com.fashion.style.service.StyleMatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StyleMatchServiceTest {

    @Autowired
    private StyleMatchService styleMatchService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Test
    void shouldRecommendSimilarProductsBasedOnStyleImageData() {
        Brand brand = brandService.createBrand(Brand.builder()
            .name("StyleMatchTestBrand")
            .slug("style-match-test-brand")
            .description("Brand for style match test")
            .build());
        Category category = categoryService.createCategory(Category.builder()
            .name("StyleMatchTestCategory")
            .slug("style-match-test-category")
            .description("Category for style match test")
            .build());

        Product product = productRepository.save(Product.builder()
                .name("White Casual Shirt")
                .slug("white-casual-shirt")
                .description("Casual everyday shirt")
                .basePrice(java.math.BigDecimal.valueOf(250000.0))
                .salePrice(java.math.BigDecimal.valueOf(220000.0))
                .category(category)
                .brand(brand)
                .material("Cotton")
                .status("ACTIVE")
                .build());

        StyleMatchRequest request = new StyleMatchRequest();
        request.setColor("White");
        request.setStyle("Casual");
        request.setCategory("Shirt");
        request.setPattern("Solid");
        request.setFashionType("Daily");

        var result = styleMatchService.recommendProducts(request);

        assertThat(result).isNotEmpty();
    }
}
