package com.fashion.chatbot.dto;

import com.fashion.search.dto.ProductSearchResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotResponse {
    private ChatbotFilters filters;
    private List<ProductSearchResponse> products;
}
