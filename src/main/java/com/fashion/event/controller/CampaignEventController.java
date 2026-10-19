package com.fashion.event.controller;

import com.fashion.event.dto.CampaignEventResponse;
import com.fashion.event.service.CampaignEventService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class CampaignEventController {
    private final CampaignEventService eventService;

    @GetMapping("/active")
    public ResponseEntity<List<CampaignEventResponse>> getActiveEvents() {
        return ResponseEntity.ok(eventService.getActiveEvents());
    }

    public CampaignEventController(CampaignEventService eventService) {
        this.eventService = eventService;
    }
}
