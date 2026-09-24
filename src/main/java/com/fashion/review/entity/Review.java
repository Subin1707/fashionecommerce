package com.fashion.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_item_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    private Long orderId;

    private Long orderItemId;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isVerified = false;

    @Column(length = 1000)
    private String sellerReply;

    private LocalDateTime repliedAt;

    @jakarta.persistence.ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @jakarta.persistence.CollectionTable(name = "review_media", joinColumns = @jakarta.persistence.JoinColumn(name = "review_id"))
    @jakarta.persistence.OrderColumn(name = "position")
    @Column(name = "data_url", columnDefinition = "text")
    @Builder.Default
    private java.util.List<String> media = new java.util.ArrayList<>();

    @Column(nullable = false)
    private Short rating;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
