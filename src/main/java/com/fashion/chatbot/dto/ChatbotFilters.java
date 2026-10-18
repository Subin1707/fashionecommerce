package com.fashion.chatbot.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class ChatbotFilters {
    private String keyword;
    private String category;
    private String style;
    private String color;
    private String size;
    private BigDecimal maxPrice;

    public ChatbotFilters() {
    }
    public ChatbotFilters(final String keyword, final String category, final String style, final String color, final String size, final BigDecimal maxPrice) {
        this.keyword = keyword;
        this.category = category;
        this.style = style;
        this.color = color;
        this.size = size;
        this.maxPrice = maxPrice;
    }
}
