package com.fashion.order;

import com.fashion.brand.entity.Brand;
import com.fashion.brand.service.BrandService;
import com.fashion.cart.entity.Cart;
import com.fashion.cart.entity.CartItem;
import com.fashion.cart.repository.CartRepository;
import com.fashion.category.entity.Category;
import com.fashion.category.service.CategoryService;
import com.fashion.order.dto.CreateOrderRequest;
import com.fashion.order.entity.Order;
import com.fashion.order.service.OrderService;
import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductVariant;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

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
    void shouldCreateOrderFromCart() {
        Long userId = 99L;

        Brand brand = brandService.createBrand(Brand.builder()
            .name("Brand")
            .slug("brand")
            .description("Brand description")
            .build());
        Category category = categoryService.createCategory(Category.builder()
            .name("Category")
            .slug("category")
            .description("Danh mục")
            .build());

        Product product = productRepository.save(Product.builder()
                .name("Áo khoác denim - Order Test 1")
                .slug("ao-khoac-denim-order-test-1")
                .description("Mô tả")
                .basePrice(java.math.BigDecimal.valueOf(390000.0))
                .salePrice(java.math.BigDecimal.valueOf(349000.0))
                .category(category)
                .brand(brand)
                .material("Denim")
                .status("ACTIVE")
                .build());
        
        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .sku("SKU-001")
                .color("Xanh")
                .size("M")
                .stockQty(10)
                .priceAdjustment(java.math.BigDecimal.ZERO)
                .build());

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

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("123 Nguyễn Huệ, Q1, TP.HCM");
        request.setPaymentMethod("COD");
        request.setShippingFee(30000.0);
        request.setDiscountAmount(0.0);

        Order order = orderService.createOrderFromCart(userId, request);

        assertThat(order).isNotNull();
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getItems()).isNotEmpty();
        assertThat(order.getTotalAmount()).isEqualTo(698000.0);
        assertThat(order.getFinalAmount()).isEqualTo(728000.0);
        assertThat(order.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(order.getOrderStatus()).isEqualTo("PENDING");
    }

    @Test
    void shouldFollowOrderStatusLifecycle() {
        Long userId = 88L;

        Brand brand = brandService.createBrand(Brand.builder()
            .name("OrderTestBrand")
            .slug("order-test-brand")
            .description("Brand for order test")
            .build());
        Category category = categoryService.createCategory(Category.builder()
            .name("OrderTestCategory")
            .slug("order-test-category")
            .description("Category for order test")
            .build());

        Product product = productRepository.save(Product.builder()
                .name("Áo sơ mi - Order Test 2")
                .slug("ao-so-mi-order-test-2")
                .description("Mô tả")
                .basePrice(java.math.BigDecimal.valueOf(250000.0))
                .salePrice(java.math.BigDecimal.valueOf(225000.0))
                .category(category)
                .brand(brand)
                .material("Cotton")
                .status("ACTIVE")
                .build());
        
        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .sku("SKU-002")
                .color("Trắng")
                .size("L")
                .stockQty(5)
                .priceAdjustment(java.math.BigDecimal.ZERO)
                .build());

        Cart cart = cartRepository.save(Cart.builder().userId(userId).build());
        cart.getItems().add(CartItem.builder()
                .cart(cart)
                .productId(product.getId())
                .variantId(variant.getId())
                .size("L")
                .quantity(1)
                .price(product.getDisplayPrice().doubleValue())
                .build());
        cartRepository.save(cart);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShippingAddress("123 Đường A");
        request.setPaymentMethod("COD");
        request.setShippingFee(30000.0);
        request.setDiscountAmount(0.0);
        
        Order order = orderService.createOrderFromCart(userId, request);

        Order confirmed = orderService.updateOrderStatus(order.getId(), "CONFIRMED");
        Order processing = orderService.updateOrderStatus(order.getId(), "PROCESSING");
        Order shipping = orderService.updateOrderStatus(order.getId(), "SHIPPING");
        Order completed = orderService.confirmReceived(order.getId(), userId);

        assertThat(confirmed.getOrderStatus()).isEqualTo("CONFIRMED");
        assertThat(shipping.getOrderStatus()).isEqualTo("SHIPPING");
        assertThat(completed.getOrderStatus()).isEqualTo("COMPLETED");
    }
}
