package com.fashion.chatbot.dto;

import com.fashion.search.dto.ProductSearchResponse;
import java.util.List;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class ChatbotResponse {
    private ChatbotFilters filters;
    private List<ProductSearchResponse> products;

    public ChatbotResponse() {
    }
    public ChatbotResponse(final ChatbotFilters filters, final List<ProductSearchResponse> products) {
        this.filters = filters;
        this.products = products;
    }
}
