package com.fashion.event.dto;

import com.fashion.event.entity.CampaignEventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CampaignEventRequest(
        @NotBlank @Size(max = 120) String eventName,
        @NotBlank @Size(max = 100) String title,
        @Size(max = 240) String message,
        @NotBlank @Size(max = 500) String desktopImageUrl,
        @Size(max = 500) String mobileImageUrl,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @Size(max = 60) String offerLabel,
        @Size(max = 50) String voucherCode,
        @NotBlank @Size(max = 300) String targetUrl,
        @NotNull CampaignEventStatus status,
        @NotNull Integer priority) {
}
