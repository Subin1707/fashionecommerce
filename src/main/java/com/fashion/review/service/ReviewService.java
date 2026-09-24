package com.fashion.review.service;

import com.fashion.product.entity.Product;
import com.fashion.product.repository.ProductRepository;
import com.fashion.review.dto.CreateReviewRequest;
import com.fashion.review.entity.Review;
import com.fashion.review.repository.ReviewRepository;
import com.fashion.messaging.EventPublisher;
import com.fashion.messaging.event.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final EventPublisher eventPublisher;
    private final com.fashion.order.repository.OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public List<Review> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, "APPROVED");
    }

    @Transactional
    public Review createReview(Long userId, Long productId, CreateReviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu đánh giá không hợp lệ");
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating phải từ 1 đến 5");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

        if (request.getOrderItemId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn sản phẩm từ đơn hàng đã hoàn thành");
        }
        var item = orderItemRepository.findForReview(request.getOrderItemId())
                .orElseThrow(() -> new IllegalArgumentException("Chi tiết đơn hàng không tồn tại"));
        boolean hasPurchased = java.util.Objects.equals(item.getOrder().getUserId(), userId)
                && java.util.Objects.equals(item.getProductId(), productId)
                && "COMPLETED".equals(item.getOrder().getOrderStatus());

        if (!hasPurchased) {
            throw new IllegalArgumentException("Chỉ customer đã mua hàng mới được đánh giá");
        }

        if (reviewRepository.existsByOrderItemId(item.getId())) {
            throw new IllegalArgumentException("Bạn đã đánh giá sản phẩm này rồi");
        }

        if (request.getComment() == null || request.getComment().isBlank() || request.getComment().length() > 1000) {
            throw new IllegalArgumentException("Nhận xét phải có từ 1 đến 1000 ký tự");
        }
        var media = validateMedia(request.getMedia());
        Review review = Review.builder()
                .userId(userId)
                .productId(product.getId())
                .orderId(item.getOrder().getId())
                .orderItemId(item.getId())
                .isVerified(true)
                .media(media)
                .rating(request.getRating().shortValue())
                .comment(request.getComment().trim())
                .status("APPROVED")
                .build();

        Review saved = reviewRepository.save(review);
        eventPublisher.publish("ReviewCreated", new ReviewCreatedEvent(
            saved.getId(), saved.getUserId(), saved.getProductId(), saved.getRating().intValue(),
            saved.getStatus(), Instant.now()));
        return saved;
    }

    @Transactional
    public Review updateReviewStatus(Long reviewId, String status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review không tồn tại"));

        if (status == null || !List.of("APPROVED", "PENDING", "REJECTED").contains(status.toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("Trạng thái review không hợp lệ");
        }

        review.setStatus(status.toUpperCase());
        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<Review> getMyReviews(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Review reply(Long reviewId, String reply) {
        if (reply == null || reply.isBlank() || reply.length() > 1000) {
            throw new IllegalArgumentException("Phản hồi phải có từ 1 đến 1000 ký tự");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Đánh giá không tồn tại"));
        review.setSellerReply(reply.trim());
        review.setRepliedAt(java.time.LocalDateTime.now());
        return reviewRepository.save(review);
    }

    private List<String> validateMedia(List<String> media) {
        if (media == null) return new java.util.ArrayList<>();
        if (media.size() > 5) throw new IllegalArgumentException("Tối đa 5 ảnh/video");
        long total = 0;
        for (String value : media) {
            if (value == null || value.length() > 8_000_000
                    || !value.matches("(?s)^data:(image/(jpeg|png|webp)|video/(mp4|webm));base64,[A-Za-z0-9+/=]+$")) {
                throw new IllegalArgumentException("Ảnh/video không hợp lệ");
            }
            byte[] bytes;
            try {
                bytes = java.util.Base64.getDecoder().decode(value.substring(value.indexOf(',') + 1));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Ảnh/video không hợp lệ");
            }
            total += bytes.length;
            if (bytes.length == 0 || total > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Tổng dung lượng ảnh/video tối đa 5 MB");
            }
        }
        return new java.util.ArrayList<>(media);
    }

    @Transactional(readOnly = true)
    public List<Review> getAllReviews() {
        return reviewRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new IllegalArgumentException("Review không tồn tại với ID: " + reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }
}
