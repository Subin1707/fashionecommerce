package com.fashion.style.dto;

import com.fashion.search.dto.ProductSearchResponse;
import java.util.List;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class VisualSearchResponse {
    private boolean enabled;
    private String message;
    private List<ProductSearchResponse> products;

    public VisualSearchResponse() {
    }
    public VisualSearchResponse(final boolean enabled, final String message, final List<ProductSearchResponse> products) {
        this.enabled = enabled;
        this.message = message;
        this.products = products;
    }
}
