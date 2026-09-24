package com.fashion.product;

import com.fashion.category.entity.Category;
import com.fashion.category.repository.CategoryRepository;
import com.fashion.category.service.CategoryService;
import com.fashion.brand.entity.Brand;
import com.fashion.brand.service.BrandService;
import com.fashion.product.entity.Product;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Test
    void shouldCreateCategoryAndProduct() {
        Category category = categoryService.createCategory(Category.builder()
            .name("Áo")
            .slug("ao")
            .description("Áo nam nữ")
            .build());
        Brand brand = brandService.createBrand(Brand.builder()
            .name("Mango")
            .slug("mango")
            .description("Thương hiệu Mango")
            .build());
        
        Product product = Product.builder()
                .name("Áo sơ mi trắng")
                .slug("ao-so-mi-trang")
                .description("Áo sơ mi basic")
                .basePrice(java.math.BigDecimal.valueOf(299000.0))
                .salePrice(java.math.BigDecimal.valueOf(249000.0))
                .category(category)
                .brand(brand)
                .material("Cotton")
                .status("ACTIVE")
                .build();

        Product saved = productService.createProduct(product);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCategory().getId()).isEqualTo(category.getId());
        assertThat(productRepository.findById(saved.getId())).isPresent();
    }
}
