package com.fashion.chatbot.service;

import com.fashion.chatbot.dto.ChatbotFilters;
import com.fashion.chatbot.dto.ChatbotResponse;
import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductVariant;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductVariantRepository;
import com.fashion.search.dto.ProductSearchRequest;
import com.fashion.search.dto.ProductSearchResponse;
import com.fashion.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FashionChatbotService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductSearchService productSearchService;
    private final ChatbotQueryParser queryParser;

    public ChatbotResponse searchProducts(String message) {
        ChatbotFilters filters = queryParser.parse(message);
        ProductSearchRequest request = ProductSearchRequest.builder()
                .keyword(filters.getKeyword())
                .category(filters.getCategory())
                .style(filters.getStyle())
                .color(filters.getColor())
                .size(filters.getSize())
                .maxPrice(filters.getMaxPrice())
                .build();

        List<ProductSearchResponse> products = productSearchService.searchProducts(request, 0, 100)
                .getContent().stream()
                .filter(product -> hasAvailableSku(product, filters))
                .toList();
        return ChatbotResponse.builder().filters(filters).products(products).build();
    }

    private boolean hasAvailableSku(ProductSearchResponse product, ChatbotFilters filters) {
        return productVariantRepository.findByProductId(product.getId()).stream()
                .anyMatch(variant -> isMatchingSku(variant, filters));
    }

    private boolean isMatchingSku(ProductVariant variant, ChatbotFilters filters) {
        if (!Boolean.TRUE.equals(variant.getIsActive()) || variant.getStockQty() == null || variant.getStockQty() <= 0) {
            return false;
        }
        boolean sizeMatches = filters.getSize() == null || filters.getSize().equalsIgnoreCase(variant.getSize());
        String color = variant.getColor() == null ? "" : variant.getColor().toLowerCase();
        boolean colorMatches = filters.getColor() == null
                || ("NEUTRAL".equalsIgnoreCase(filters.getColor())
                    ? color.contains("black") || color.contains("white") || color.contains("gray")
                        || color.contains("grey") || color.contains("beige") || color.contains("navy")
                    : color.contains(filters.getColor().toLowerCase()));
        return sizeMatches && colorMatches;
    }

    public String chat(Long userId, String message) {
        if (message == null || message.isBlank()) {
            return "Tôi cần thông tin rõ hơn để hỗ trợ bạn.";
        }

        String lower = message.toLowerCase();

        if (lower.contains("váy") && lower.contains("đen")) {
            List<Product> matches = productRepository.findAll().stream()
                    .filter(product -> product.getStatus() != null && "ACTIVE".equalsIgnoreCase(product.getStatus()))
                    .filter(product -> product.getName() != null && product.getName().toLowerCase().contains("dress"))
                    .limit(3)
                    .toList();

            if (!matches.isEmpty()) {
                return "Tôi tìm thấy một số mẫu váy màu đen phù hợp với yêu cầu của bạn.";
            }
            return "Tôi chưa tìm thấy mẫu váy đen phù hợp ngay lúc này, bạn có muốn tôi gợi ý phong cách khác?";
        }

        if (lower.contains("size") || lower.contains("kích thước")) {
            return "Bạn có thể chia sẻ chiều cao, cân nặng, tuổi và số đo cơ thể để tôi gợi ý size phù hợp.";
        }

        if (lower.contains("giá") || lower.contains("price")) {
            return "Tôi có thể giúp bạn tìm sản phẩm theo mức giá phù hợp. Bạn muốn khoảng giá nào?";
        }

        if (lower.contains("đơn hàng") || lower.contains("status")) {
            return "Bạn có thể kiểm tra trạng thái đơn hàng trong mục đơn hàng của tài khoản.";
        }

        return "Tôi có thể hỗ trợ bạn tìm sản phẩm, tư vấn size, outfit và thông tin sản phẩm.";
    }
}
