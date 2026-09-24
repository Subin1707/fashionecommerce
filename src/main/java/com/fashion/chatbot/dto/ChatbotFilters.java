package com.fashion.chatbot.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotFilters {
    private String keyword;
    private String category;
    private String style;
    private String color;
    private String size;
    private BigDecimal maxPrice;
}
