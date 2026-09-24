package com.fashion.chatbot;

import com.fashion.chatbot.service.FashionChatbotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FashionChatbotServiceTest {

    @Autowired
    private FashionChatbotService fashionChatbotService;

    @Test
    void shouldHandleBlackDressSearchQuery() {
        String response = fashionChatbotService.chat(1L, "Tôi muốn tìm một chiếc váy màu đen.");

        assertThat(response).contains("váy");
        assertThat(response.toLowerCase()).contains("đen");
    }
}
