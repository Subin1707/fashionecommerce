package com.fashion.cart;

import com.fashion.brand.entity.Brand;
import com.fashion.brand.service.BrandService;
import com.fashion.cart.dto.CartResponse;
import com.fashion.cart.dto.CartItemRequest;
import com.fashion.cart.entity.Cart;
import com.fashion.cart.service.CartService;
import com.fashion.category.entity.Category;
import com.fashion.category.service.CategoryService;
import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductImage;
import com.fashion.product.entity.ProductVariant;
import com.fashion.product.repository.ProductImageRepository;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Test
    void shouldAddItemToCart() {
        Long userId = 77L;

        Brand brand = brandService.createBrand(Brand.builder()
            .name("CartTestBrand")
            .slug("cart-test-brand")
            .description("Brand for cart test")
            .build());
        Category category = categoryService.createCategory(Category.builder()
            .name("CartTestCategory")
            .slug("cart-test-category")
            .description("Category for cart test")
            .build());
        
        Product product = productRepository.save(Product.builder()
                .name("Áo khoác denim - Cart Test")
                .slug("ao-khoac-denim-cart-test")
                .description("Mô tả")
                .basePrice(BigDecimal.valueOf(390000.0))
                .salePrice(BigDecimal.valueOf(349000.0))
                .category(category)
                .brand(brand)
                .material("Denim")
                .status("ACTIVE")
                .build());
        
        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .sku("SKU-CART-001")
                .color("Xanh")
                .size("28")
                .stockQty(10)
                .priceAdjustment(BigDecimal.ZERO)
                .build());

        productImageRepository.save(ProductImage.builder()
                .product(product)
                .variant(variant)
                .imageUrl("https://example.com/cart-test-image.jpg")
                .altText("Cart test image")
                .isPrimary(true)
                .displayOrder(1)
                .build());

        CartItemRequest request = new CartItemRequest();
        request.setProductId(product.getId());
        request.setVariantId(variant.getId());
        request.setSize("28");
        request.setQuantity(2);

        Cart cart = cartService.addItemToCart(userId, request);

        assertThat(cart).isNotNull();
        assertThat(cart.getItems()).isNotEmpty();
        assertThat(cart.getItems().getFirst().getProductId()).isEqualTo(product.getId());

        CartResponse response = cartService.toResponse(cart);
        assertThat(response.getItems().getFirst().getProductName()).isEqualTo(product.getName());
        assertThat(response.getItems().getFirst().getImageUrl()).isEqualTo("https://example.com/cart-test-image.jpg");
    }
}
