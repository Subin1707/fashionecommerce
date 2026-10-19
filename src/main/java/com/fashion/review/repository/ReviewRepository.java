package com.fashion.review.repository;

import com.fashion.review.entity.Review;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    interface RatingSummary {
        Double getAverageRating();
        Long getTotalReviews();
    }

    interface ProductRatingSummary extends RatingSummary {
        Long getProductId();
    }

    @Query("select coalesce(avg(r.rating), 0.0) as averageRating, count(r) as totalReviews from Review r where r.productId = :productId and r.status = 'APPROVED'")
    RatingSummary summarize(@Param("productId") Long productId);

    @Query("""
            select r.productId as productId,
                   coalesce(avg(r.rating), 0.0) as averageRating,
                   count(r) as totalReviews
            from Review r
            where r.productId in :productIds and r.status = 'APPROVED'
            group by r.productId
            """)
    List<ProductRatingSummary> summarizeProducts(@Param("productIds") Collection<Long> productIds);

    List<Review> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, String status);
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByOrderItemId(Long orderItemId);
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi JOIN oi.order o "
            + "WHERE o.userId = :userId AND o.orderStatus = 'COMPLETED' AND oi.productId = :productId")
    boolean existsCompletedPurchase(@Param("userId") Long userId, @Param("productId") Long productId);
}
