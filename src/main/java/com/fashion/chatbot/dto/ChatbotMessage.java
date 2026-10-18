package com.fashion.chatbot.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotMessage {
    private String message;

    public ChatbotMessage() {
    }
    public ChatbotMessage(final String message) {
        this.message = message;
    }
}
