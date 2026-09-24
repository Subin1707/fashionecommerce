package com.fashion.review;

import com.fashion.messaging.EventPublisher;
import com.fashion.order.entity.Order;
import com.fashion.order.entity.OrderItem;
import com.fashion.order.repository.OrderItemRepository;
import com.fashion.product.entity.Product;
import com.fashion.product.repository.ProductRepository;
import com.fashion.review.dto.CreateReviewRequest;
import com.fashion.review.entity.Review;
import com.fashion.review.repository.ReviewRepository;
import com.fashion.review.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewEligibilityTest {
    @Mock ReviewRepository reviews;
    @Mock ProductRepository products;
    @Mock OrderItemRepository items;
    @Mock EventPublisher events;
    ReviewService service;

    @BeforeEach void setup() { service = new ReviewService(reviews, products, events, items); }

    CreateReviewRequest request() {
        return new CreateReviewRequest(5, "Good fit", 7L, List.of());
    }

    void purchase(String status, Long owner, Long product) {
        when(products.findById(2L)).thenReturn(Optional.of(Product.builder().id(2L).build()));
        when(items.findForReview(7L)).thenReturn(Optional.of(OrderItem.builder().id(7L).productId(product)
                .order(Order.builder().id(3L).userId(owner).orderStatus(status).build()).build()));
    }

    @Test void rejectsOtherCustomersOrder() {
        purchase("COMPLETED", 99L, 2L);
        assertThatThrownBy(() -> service.createReview(1L, 2L, request())).isInstanceOf(IllegalArgumentException.class);
        verify(reviews, never()).save(any());
    }

    @Test void rejectsDifferentProduct() {
        purchase("COMPLETED", 1L, 8L);
        assertThatThrownBy(() -> service.createReview(1L, 2L, request())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejectsUnfinishedOrder() {
        purchase("DELIVERED", 1L, 2L);
        assertThatThrownBy(() -> service.createReview(1L, 2L, request())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejectsDuplicateOrderItem() {
        purchase("COMPLETED", 1L, 2L);
        when(reviews.existsByOrderItemId(7L)).thenReturn(true);
        assertThatThrownBy(() -> service.createReview(1L, 2L, request())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void acceptsNewPurchaseAndLinksExactOrderItem() {
        purchase("COMPLETED", 1L, 2L);
        when(reviews.save(any())).thenAnswer(call -> { Review r = call.getArgument(0); r.setId(10L); return r; });
        Review result = service.createReview(1L, 2L, request());
        assertThat(result.getOrderId()).isEqualTo(3L);
        assertThat(result.getOrderItemId()).isEqualTo(7L);
        assertThat(result.getIsVerified()).isTrue();
        verify(reviews, never()).findByUserIdAndProductId(any(), any());
    }

    @Test void rejectsInvalidRating() {
        var request = request(); request.setRating(6);
        assertThatThrownBy(() -> service.createReview(1L, 2L, request)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(items, reviews);
    }

    @Test void rejectsBlankComment() {
        purchase("COMPLETED", 1L, 2L);
        var request = request(); request.setComment("  ");
        assertThatThrownBy(() -> service.createReview(1L, 2L, request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejectsUnsupportedMedia() {
        purchase("COMPLETED", 1L, 2L);
        var request = request(); request.setMedia(List.of("data:text/html;base64,SGVsbG8="));
        assertThatThrownBy(() -> service.createReview(1L, 2L, request)).isInstanceOf(IllegalArgumentException.class);
    }
}
