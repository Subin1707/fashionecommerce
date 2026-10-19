package com.fashion.event.controller;

import com.fashion.event.dto.CampaignEventRequest;
import com.fashion.event.dto.CampaignEventResponse;
import com.fashion.event.dto.CampaignImageResponse;
import com.fashion.event.service.CampaignEventService;
import com.fashion.event.service.CampaignImageStorageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCampaignEventController {
    private final CampaignEventService eventService;
    private final CampaignImageStorageService imageStorage;

    @GetMapping
    public ResponseEntity<List<CampaignEventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @PostMapping
    public ResponseEntity<CampaignEventResponse> create(@Valid @RequestBody CampaignEventRequest request) {
        return ResponseEntity.ok(eventService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignEventResponse> update(@PathVariable Long id,
            @Valid @RequestBody CampaignEventRequest request) {
        return ResponseEntity.ok(eventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CampaignImageResponse> uploadImage(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(new CampaignImageResponse(imageStorage.store(file)));
    }

    public AdminCampaignEventController(CampaignEventService eventService, CampaignImageStorageService imageStorage) {
        this.eventService = eventService;
        this.imageStorage = imageStorage;
    }
}
