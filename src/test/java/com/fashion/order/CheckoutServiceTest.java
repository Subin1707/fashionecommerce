package com.fashion.order;

import com.fashion.brand.entity.Brand;
import com.fashion.brand.service.BrandService;
import com.fashion.cart.entity.Cart;
import com.fashion.cart.entity.CartItem;
import com.fashion.cart.repository.CartRepository;
import com.fashion.category.entity.Category;
import com.fashion.category.service.CategoryService;
import com.fashion.order.dto.CheckoutRequest;
import com.fashion.order.entity.Order;
import com.fashion.order.enums.OrderStatus;
import com.fashion.order.service.CheckoutService;
import com.fashion.product.entity.Product;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CheckoutServiceTest {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Test
    void shouldCreateOrderFromCheckout() {
        Long userId = 101L;

        Brand brand = brandService.createBrand(Brand.builder()
            .name("CheckoutTestBrand")
            .slug("checkout-test-brand")
            .description("Brand for checkout test")
            .build());
        Category category = categoryService.createCategory(Category.builder()
            .name("CheckoutTestCategory")
            .slug("checkout-test-category")
            .description("Category for checkout test")
            .build());

        Product product = productRepository.save(Product.builder()
                .name("Áo khoác denim - Checkout Test")
                .slug("ao-khoac-denim-checkout-test")
                .description("Mô tả")
                .basePrice(java.math.BigDecimal.valueOf(390000.0))
                .salePrice(java.math.BigDecimal.valueOf(349000.0))
                .category(category)
                .brand(brand)
                .material("Denim")
                .status("ACTIVE")
                .build());
        
        com.fashion.product.entity.ProductVariant variant = new com.fashion.product.entity.ProductVariant();
        variant.setProduct(product);
        variant.setSku("SKU-003");
        variant.setColor("Xanh");
        variant.setSize("M");
        variant.setStockQty(10);
        variant.setPriceAdjustment(java.math.BigDecimal.ZERO);
        variant = productVariantRepository.save(variant);

        Cart cart = cartRepository.save(Cart.builder().userId(userId).build());
        cart.getItems().add(CartItem.builder()
                .cart(cart)
                .productId(product.getId())
                .variantId(variant.getId())
                .size("M")
                .quantity(2)
                .price(product.getDisplayPrice().doubleValue())
                .build());
        cartRepository.save(cart);

        CheckoutRequest request = new CheckoutRequest();
        request.setShippingAddress("123 Nguyễn Huệ, Q1, TP.HCM");
        request.setPaymentMethod("COD");
        request.setShippingFee(30000.0);
        request.setDiscountAmount(999999999.0);

        Order order = checkoutService.checkout(userId, request);

        assertThat(order).isNotNull();
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getTotalAmount()).isEqualTo(698000.0);
        assertThat(order.getFinalAmount()).isEqualTo(728000.0);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(order.getItems()).isNotEmpty();
    }
}
