package com.fashion.order.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_requests")
@Getter @Setter @Builder @AllArgsConstructor
public class ReturnRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "order_item_id", nullable = false) private Long orderItemId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 20) private String requestType;
    @Column(nullable = false) private Integer quantity;
    @Column(nullable = false, length = 500) private String reason;
    @Column(nullable = false, length = 20) private String status;
    @Column(length = 500) private String adminNote;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "return_request_evidence", joinColumns = @JoinColumn(name = "return_request_id"))
    @Column(name = "file_url", nullable = false, length = 255)
    @Builder.Default private List<String> evidenceFiles = new ArrayList<>();
    @CreationTimestamp @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(nullable = false) private LocalDateTime updatedAt;
    public ReturnRequest() {}
}
