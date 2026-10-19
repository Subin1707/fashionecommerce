package com.fashion.admin;

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
import com.fashion.security.JwtService;
import com.fashion.user.entity.Role;
import com.fashion.user.entity.User;
import com.fashion.user.repository.RoleRepository;
import com.fashion.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ADMIN")));
        String adminEmail = "dashboard-admin-" + UUID.randomUUID() + "@example.com";
        User admin = userRepository.save(User.builder()
                .fullName("Dashboard Admin")
                .email(adminEmail)
                .password("not-used")
                .role(adminRole)
                .status("ACTIVE")
                .build());
        adminToken = jwtService.generateToken(admin.getEmail(), "ADMIN");
    }

    @Test
    void returnsDashboardWhenDatabaseHasNoOrders() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(0))
                .andExpect(jsonPath("$.totalOrders").value(0))
                .andExpect(jsonPath("$.totalStock").value(0))
                .andExpect(jsonPath("$.averageOrderValue").value(0));
    }

    @Test
    void returnsInventorySummariesWithAggregatedStock() throws Exception {
        Brand brand = brandRepository.save(Brand.builder()
                .name("Inventory Test Brand")
                .slug("inventory-test-brand")
                .build());
        Category category = categoryRepository.save(Category.builder()
                .name("Inventory Test Category")
                .slug("inventory-test-category")
                .build());
        Product product = productRepository.saveAndFlush(Product.builder()
                .brand(brand)
                .category(category)
                .name("Inventory Test Product")
                .slug("inventory-test-product")
                .basePrice(BigDecimal.valueOf(100))
                .build());
        productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .sku("INV-TEST-SKU")
                .color("Pink")
                .size("M")
                .stockQty(7)
                .build());
        productImageRepository.save(ProductImage.builder()
                .product(product)
                .imageUrl("/test/inventory-product.png")
                .isPrimary(true)
                .displayOrder(0)
                .build());

        mockMvc.perform(get("/api/admin/products/inventory?page=0&size=100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + product.getId() + ")].name")
                        .value(org.hamcrest.Matchers.hasItem("Inventory Test Product")))
                .andExpect(jsonPath("$.content[?(@.id == " + product.getId() + ")].brandName")
                        .value(org.hamcrest.Matchers.hasItem("Inventory Test Brand")))
                .andExpect(jsonPath("$.content[?(@.id == " + product.getId() + ")].categoryName")
                        .value(org.hamcrest.Matchers.hasItem("Inventory Test Category")))
                .andExpect(jsonPath("$.content[?(@.id == " + product.getId() + ")].totalStock")
                        .value(org.hamcrest.Matchers.hasItem(7)));

        mockMvc.perform(get("/api/search?size_param=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + product.getId() + ")].primaryImageUrl")
                        .value(org.hamcrest.Matchers.hasItem("/test/inventory-product.png")))
                .andExpect(jsonPath("$.content[?(@.id == " + product.getId() + ")].totalStock")
                        .value(org.hamcrest.Matchers.hasItem(7)));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + product.getId() + ")].primaryImageUrl")
                        .value(org.hamcrest.Matchers.hasItem("/test/inventory-product.png")))
                .andExpect(jsonPath("$[?(@.id == " + product.getId() + ")].totalStock")
                        .value(org.hamcrest.Matchers.hasItem(7)))
                .andExpect(jsonPath("$[?(@.id == " + product.getId() + ")].averageRating")
                        .value(org.hamcrest.Matchers.hasItem(0.0)));

        mockMvc.perform(get("/api/products/" + product.getId() + "/similar"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/products?page=0&size=100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + product.getId() + ")].variants[*].sku")
                        .value(org.hamcrest.Matchers.hasItem("INV-TEST-SKU")))
                .andExpect(jsonPath("$.content[?(@.id == " + product.getId() + ")].images[*].imageUrl")
                        .value(org.hamcrest.Matchers.hasItem("/test/inventory-product.png")));
    }
}
