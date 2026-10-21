package com.fashion.order.controller;

import com.fashion.order.dto.CreateReturnRequest;
import com.fashion.order.entity.ReturnRequest;
import com.fashion.order.service.ReturnRequestService;
import com.fashion.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReturnRequestController {
    private final ReturnRequestService service;
    public ReturnRequestController(ReturnRequestService service) { this.service = service; }

    @PostMapping("/orders/{orderId}/return-requests")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ReturnRequest create(@PathVariable Long orderId, @Valid @ModelAttribute CreateReturnRequest body,
            @RequestPart("files") List<MultipartFile> files, Authentication auth) {
        return service.create(((UserPrincipal) auth.getPrincipal()).getUserId(), orderId, body, files);
    }

    @GetMapping("/orders/return-requests")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public List<ReturnRequest> mine(Authentication auth) { return service.getMine(((UserPrincipal) auth.getPrincipal()).getUserId()); }

    @GetMapping("/orders/return-requests/{id}/evidence/{filename}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER','ROLE_ADMIN')")
    public ResponseEntity<Resource> evidence(@PathVariable Long id, @PathVariable String filename, Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        Long userId = auth.getPrincipal() instanceof UserPrincipal principal ? principal.getUserId() : null;
        Path file = service.getEvidence(userId, admin, id, filename);
        Resource resource = new PathResource(file);
        MediaType type = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).body(resource);
    }

    @GetMapping("/admin/return-requests")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<ReturnRequest> all() { return service.getAll(); }

    @PatchMapping("/admin/return-requests/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ReturnRequest review(@PathVariable Long id, @RequestBody ReviewBody body) {
        return service.review(id, body.status(), body.adminNote());
    }

    public record ReviewBody(String status, String adminNote) {}
}
