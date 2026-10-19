package com.fashion.chatbot.dto;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotMessage {
    private String message;
    private Long productId;
    private List<ChatMessage> history;

    @Getter
    @Setter
    public static class ChatMessage {
        private String role;
        private String content;
    }

    public ChatbotMessage() {
    }
    public ChatbotMessage(final String message) {
        this.message = message;
    }
}
