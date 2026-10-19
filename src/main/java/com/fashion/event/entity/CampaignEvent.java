package com.fashion.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "campaign_events")
@Getter
@Setter
@Builder
public class CampaignEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String eventName;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 240)
    private String message;

    @Column(nullable = false, length = 500)
    private String desktopImageUrl;

    @Column(length = 500)
    private String mobileImageUrl;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(length = 60)
    private String offerLabel;

    @Column(length = 50)
    private String voucherCode;

    @Column(nullable = false, length = 300)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignEventStatus status;

    @Column(nullable = false)
    private Integer priority;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public CampaignEvent() {
    }

    public CampaignEvent(Long id, String eventName, String title, String message, String desktopImageUrl,
            String mobileImageUrl, LocalDateTime startAt, LocalDateTime endAt, String offerLabel,
            String voucherCode, String targetUrl, CampaignEventStatus status, Integer priority,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.eventName = eventName;
        this.title = title;
        this.message = message;
        this.desktopImageUrl = desktopImageUrl;
        this.mobileImageUrl = mobileImageUrl;
        this.startAt = startAt;
        this.endAt = endAt;
        this.offerLabel = offerLabel;
        this.voucherCode = voucherCode;
        this.targetUrl = targetUrl;
        this.status = status;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
