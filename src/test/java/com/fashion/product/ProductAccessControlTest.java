package com.fashion.product;

import com.fashion.auth.dto.LoginRequest;
import com.fashion.auth.dto.RegisterRequest;
import com.fashion.auth.service.AuthService;
import com.fashion.brand.entity.Brand;
import com.fashion.brand.service.BrandService;
import com.fashion.category.entity.Category;
import com.fashion.category.service.CategoryService;
import com.fashion.product.entity.Product;
import com.fashion.product.service.ProductService;
import com.fashion.user.entity.Role;
import com.fashion.user.repository.RoleRepository;
import com.fashion.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProductAccessControlTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Test
    void shouldAllowCustomerToViewProducts() {
        roleRepository.findByName("CUSTOMER").orElseGet(() -> roleRepository.save(new Role("CUSTOMER")));

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFullName("Customer View");
        registerRequest.setEmail("customer-view@example.com");
        registerRequest.setPassword("123456");
        registerRequest.setPhone("0901111222");
        registerRequest.setAddress("Da Nang");

        authService.register(registerRequest);

        Brand brand = brandService.createBrand(Brand.builder()
            .name("ProductAccessTestBrand")
            .slug("product-access-test-brand")
            .description("Brand for product access test")
            .build());
        Category category = categoryService.createCategory(Category.builder()
            .name("ProductAccessTestCategory")
            .slug("product-access-test-category")
            .description("Category for product access test")
            .build());

        Product product = Product.builder()
                .name("Áo thun cotton - Access Test")
                .slug("ao-thun-cotton-access-test")
                .description("Mô tả")
                .basePrice(java.math.BigDecimal.valueOf(250000.0))
                .salePrice(java.math.BigDecimal.valueOf(220000.0))
                .category(category)
                .brand(brand)
                .material("Cotton")
                .status("ACTIVE")
                .build();

        Product savedProduct = productService.createProduct(product);

        assertThat(savedProduct.getId()).isNotNull();
        assertThat(productService.getAllProducts()).isNotEmpty();
        assertThat(userRepository.findByEmail("customer-view@example.com")).isPresent();
    }
}
