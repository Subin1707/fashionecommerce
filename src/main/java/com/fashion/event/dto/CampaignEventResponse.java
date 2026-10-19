package com.fashion.event.dto;

import com.fashion.event.entity.CampaignEvent;
import com.fashion.event.entity.CampaignEventStatus;
import java.time.LocalDateTime;

public record CampaignEventResponse(
        Long id,
        String eventName,
        String title,
        String message,
        String desktopImageUrl,
        String mobileImageUrl,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String offerLabel,
        String voucherCode,
        String targetUrl,
        CampaignEventStatus status,
        Integer priority) {
    public static CampaignEventResponse from(CampaignEvent event, CampaignEventStatus status) {
        return new CampaignEventResponse(event.getId(), event.getEventName(), event.getTitle(), event.getMessage(),
                event.getDesktopImageUrl(), event.getMobileImageUrl(), event.getStartAt(), event.getEndAt(),
                event.getOfferLabel(), event.getVoucherCode(), event.getTargetUrl(), status, event.getPriority());
    }
}
