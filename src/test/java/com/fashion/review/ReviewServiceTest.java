package com.fashion.review;

import com.fashion.brand.entity.Brand;
import com.fashion.brand.service.BrandService;
import com.fashion.category.entity.Category;
import com.fashion.category.service.CategoryService;
import com.fashion.order.entity.Order;
import com.fashion.order.entity.OrderItem;
import com.fashion.order.repository.OrderRepository;
import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductVariant;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductVariantRepository;
import com.fashion.review.dto.CreateReviewRequest;
import com.fashion.review.entity.Review;
import com.fashion.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private com.fashion.review.repository.ReviewRepository reviewRepository;

    @Test
    void shouldCreateReviewAfterCustomerPurchasedProduct() {
        Brand brand = brandService.createBrand(Brand.builder()
            .name("ReviewTestBrand")
            .slug("review-test-brand")
            .description("Brand for review test")
            .build());
        Category category = categoryService.createCategory(Category.builder()
            .name("ReviewTestCategory")
            .slug("review-test-category")
            .description("Category for review test")
            .build());

        Product product = productRepository.save(Product.builder()
                .name("Giày chạy bộ")
                .slug("giay-chay-bo")
                .description("Mô tả")
                .basePrice(java.math.BigDecimal.valueOf(800000.0))
                .salePrice(java.math.BigDecimal.valueOf(750000.0))
                .category(category)
                .brand(brand)
                .material("Mesh")
                .status("ACTIVE")
                .build());

        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .sku("GIAY-CHAY-BO-REVIEW-L")
                .color("Black")
                .size("L")
                .stockQty(10)
                .priceAdjustment(java.math.BigDecimal.ZERO)
                .build());

        Order order = orderRepository.save(Order.builder()
                .userId(400L)
                .totalAmount(750000.0)
                .shippingFee(30000.0)
                .discountAmount(0.0)
                .finalAmount(780000.0)
                .shippingAddress("123 ABC")
                .paymentMethod("COD")
                .paymentStatus("PAID")
                .orderStatus("COMPLETED")
                .build());

        OrderItem item = OrderItem.builder()
                .order(order)
                .productId(product.getId())
                .variantId(variant.getId())
                .sku(variant.getSku())
                .productName(product.getName())
                .size("L")
                .quantity(1)
                .price(750000.0)
                .build();
        order.getItems().add(item);
        order = orderRepository.save(order);

        CreateReviewRequest request = new CreateReviewRequest(5, "Sản phẩm rất tốt");
        request.setOrderItemId(order.getItems().getFirst().getId());
        Long productId = product.getId();
        assertThatThrownBy(() -> reviewService.createReview(999L, productId, request))
                .isInstanceOf(IllegalArgumentException.class);
        order.setOrderStatus("DELIVERED");
        orderRepository.save(order);
        assertThatThrownBy(() -> reviewService.createReview(400L, productId, request))
                .isInstanceOf(IllegalArgumentException.class);
        order.setOrderStatus("COMPLETED");
        orderRepository.save(order);
        Review review = reviewService.createReview(400L, productId, request);

        assertThat(review).isNotNull();
        assertThat(review.getRating()).isEqualTo((short) 5);
        assertThat(review.getComment()).isEqualTo("Sản phẩm rất tốt");
        assertThat(review.getStatus()).isEqualTo("APPROVED");
        assertThat(review.getOrderId()).isEqualTo(order.getId());
        assertThat(review.getOrderItemId()).isEqualTo(request.getOrderItemId());
        assertThat(review.getIsVerified()).isTrue();
        assertThatThrownBy(() -> reviewService.createReview(400L, productId, request))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(reviewRepository.summarize(productId).getAverageRating()).isEqualTo(5.0);
        reviewService.reply(review.getId(), "Cảm ơn bạn!");
        assertThat(reviewService.getReviewsByProduct(productId).getFirst().getSellerReply()).isEqualTo("Cảm ơn bạn!");
        reviewService.updateReviewStatus(review.getId(), "REJECTED");
        assertThat(reviewService.getReviewsByProduct(productId)).isEmpty();
        assertThat(reviewRepository.summarize(productId).getTotalReviews()).isZero();
    }
}
