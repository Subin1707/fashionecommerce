package com.fashion.chatbot.controller;

import com.fashion.chatbot.dto.ChatbotMessage;
import com.fashion.chatbot.dto.ChatbotResponse;
import com.fashion.chatbot.service.FashionChatbotService;
import com.fashion.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final FashionChatbotService fashionChatbotService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<String> chat(@RequestBody ChatbotMessage message, Authentication authentication) {
        return ResponseEntity.ok(fashionChatbotService.chat(userId(authentication), message.getMessage()));
    }

    @PostMapping("/products")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<ChatbotResponse> searchProducts(@RequestBody ChatbotMessage message) {
        return ResponseEntity.ok(fashionChatbotService.searchProducts(message.getMessage()));
    }

    private Long userId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getUserId();
    }
}
