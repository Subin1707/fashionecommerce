package com.fashion.chatbot.service;

import com.fashion.chatbot.dto.ChatbotFilters;
import com.fashion.chatbot.dto.ChatbotMessage;
import com.fashion.chatbot.dto.ChatbotResponse;
import com.fashion.product.entity.Product;
import com.fashion.product.entity.ProductVariant;
import com.fashion.product.repository.ProductRepository;
import com.fashion.product.repository.ProductVariantRepository;
import com.fashion.search.dto.ProductSearchRequest;
import com.fashion.search.dto.ProductSearchResponse;
import com.fashion.search.service.ProductSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FashionChatbotService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductSearchService productSearchService;
    private final ChatbotQueryParser queryParser;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    @Value("${fashion.groq.api-key:}")
    private String groqApiKey;
    @Value("${fashion.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    public FashionChatbotService(ProductRepository productRepository, ProductVariantRepository productVariantRepository,
            ProductSearchService productSearchService, ChatbotQueryParser queryParser, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productSearchService = productSearchService;
        this.queryParser = queryParser;
        this.objectMapper = objectMapper;
    }

    public ChatbotResponse searchProducts(String message, Long productId) {
        return findProducts(message, productId);
    }

    private ChatbotResponse findProducts(String message, Long productId) {
        if (productId != null) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) return ChatbotResponse.builder().products(List.of()).build();
            List<ProductSearchResponse> matches = productSearchService.searchProducts(
                            ProductSearchRequest.builder().keyword(product.getName()).build(), 0, 100).getContent().stream()
                    .filter(item -> productId.equals(item.getId())).toList();
            return ChatbotResponse.builder().products(matches).build();
        }

        ChatbotFilters filters = queryParser.parse(message);
        if (filters.getCategory() != null) {
            List<ProductSearchResponse> matches = categorySlugs(filters.getCategory()).stream()
                    .flatMap(slug -> productSearchService.searchProducts(ProductSearchRequest.builder()
                            .category(slug).color(searchColor(filters.getColor())).size(filters.getSize())
                            .maxPrice(filters.getMaxPrice()).build(), 0, 50).getContent().stream())
                    .filter(product -> product.getTotalStock() != null && product.getTotalStock() > 0)
                    .distinct().toList();
            return ChatbotResponse.builder().filters(filters).products(matches).build();
        }

        String normalizedMessage = normalize(message);
        boolean outfitRequest = filters.getStyle() != null || containsAny(normalizedMessage, "outfit", "phoi do", "set do", "mac gi");
        if (outfitRequest) {
            List<ProductSearchResponse> outfitItems = List.of("ao-thun", "ao-khoac", "quan", "dam").stream()
                    .flatMap(slug -> productSearchService.searchProducts(ProductSearchRequest.builder()
                            .category(slug).maxPrice(filters.getMaxPrice()).build(), 0, 10).getContent().stream())
                    .filter(product -> product.getTotalStock() != null && product.getTotalStock() > 0)
                    .distinct().limit(8).toList();
            return ChatbotResponse.builder().filters(filters).products(outfitItems).build();
        }
        ProductSearchRequest request = ProductSearchRequest.builder()
                .keyword(filters.getKeyword()).style(filters.getStyle())
                .color(searchColor(filters.getColor())).size(filters.getSize()).maxPrice(filters.getMaxPrice()).build();
        if (filters.getKeyword() == null && filters.getStyle() == null && filters.getColor() == null
                && filters.getSize() == null && filters.getMaxPrice() == null) {
            return ChatbotResponse.builder().filters(filters).products(List.of()).build();
        }
        List<ProductSearchResponse> matches = productSearchService.searchProducts(request, 0, 100).getContent().stream()
                .filter(product -> hasAvailableSku(product, filters)).toList();
        return ChatbotResponse.builder().filters(filters).products(matches).build();
    }

    private List<String> categorySlugs(String category) {
        return switch (category) {
            case "APPAREL_TOP" -> List.of("ao-thun", "ao-khoac");
            case "BOTTOM" -> List.of("quan");
            case "DRESS" -> List.of("dam");
            case "SHOES", "BAGS" -> List.of("phu-kien");
            default -> List.of(category.toLowerCase(java.util.Locale.ROOT));
        };
    }

    private String searchColor(String color) {
        if (color == null || "neutral".equalsIgnoreCase(color)) return null;
        return switch (normalize(color)) {
            case "black" -> "black";
            case "white" -> "white";
            case "blue" -> "blue";
            case "red" -> "red";
            default -> normalize(color);
        };
    }

    private boolean hasAvailableSku(ProductSearchResponse product, ChatbotFilters filters) {
        return productVariantRepository.findByProductId(product.getId()).stream().anyMatch(variant -> isMatchingSku(variant, filters));
    }

    private boolean isMatchingSku(ProductVariant variant, ChatbotFilters filters) {
        if (!Boolean.TRUE.equals(variant.getIsActive()) || variant.getStockQty() == null || variant.getStockQty() <= 0) return false;
        boolean sizeMatches = filters.getSize() == null || filters.getSize().equalsIgnoreCase(variant.getSize());
        String color = normalize(variant.getColor());
        String requested = normalize(filters.getColor());
        boolean colorMatches = filters.getColor() == null || switch (requested) {
            case "black" -> color.contains("black") || color.contains("den");
            case "white" -> color.contains("white") || color.contains("trang") || color.contains("ivory");
            case "blue" -> color.contains("blue") || color.contains("xanh");
            case "red" -> color.contains("red") || color.contains("do");
            default -> color.contains(requested);
        };
        return sizeMatches && colorMatches;
    }

    public String chat(Long userId, String message) {
        return chat(userId, new ChatbotMessage(message)).getAnswer();
    }

    public ChatbotResponse chat(Long userId, ChatbotMessage request) {
        String message = request == null ? null : request.getMessage();
        if (message == null || message.isBlank()) {
            return ChatbotResponse.builder().answer("Mình cần thêm thông tin để hỗ trợ bạn.").products(List.of()).build();
        }
        String searchText = contextualize(message, request.getHistory());
        ChatbotResponse matches = findProducts(searchText, request.getProductId());
        String answer;
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            try {
                answer = askGroq(request, searchText, matches.getProducts());
            } catch (Exception exception) {
                log.warn("Groq chat unavailable; using store guidance fallback: {}", exception.getMessage());
                answer = fallbackAdvice(searchText, request.getProductId(), matches);
            }
        } else {
            answer = fallbackAdvice(searchText, request.getProductId(), matches);
        }
        return ChatbotResponse.builder().answer(answer).filters(matches.getFilters()).products(matches.getProducts()).build();
    }

    private String contextualize(String message, List<ChatbotMessage.ChatMessage> history) {
        String normalized = normalize(message);
        boolean followUp = containsAny(normalized, "den", "trang", "xanh", "do", "size", "mau", "gia", "re", "duoi", "moi", "khac")
                && !containsAny(normalized, "ao", "vay", "dam", "quan", "giay", "tui", "vi", "bomber", "hoodie", "cotton", "linen");
        if (!followUp || history == null) return message;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatbotMessage.ChatMessage item = history.get(i);
            if (item != null && "user".equals(item.getRole()) && item.getContent() != null) return item.getContent() + " " + message;
        }
        return message;
    }

    private String askGroq(ChatbotMessage request, String searchText, List<ProductSearchResponse> products) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        String catalog = products == null ? "" : products.stream().limit(12)
                .map(product -> String.format("%s | %s | category %s | material %s | fit %s | price %s | colors %s | sizes %s | stock %s",
                        product.getId(), safe(product.getName()), safe(product.getCategoryName()), safe(product.getMaterial()), safe(product.getFit()),
                        product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice(), safe(product.getColors()), safe(product.getSizes()), safe(product.getTotalStock())))
                .reduce((left, right) -> left + System.lineSeparator() + right).orElse("No catalog match.");
        String prompt = "You are a Vietnamese fashion store shopping assistant. Answer the actual question in natural Vietnamese. Use this live catalog and do not invent product facts, stock, prices, or policies. Help with product search, comparison, materials, fit, size, outfits, cart, vouchers, checkout, payments and order navigation. For private order status, direct the customer to Orders. Current request: " + searchText + System.lineSeparator() + "Catalog:" + System.lineSeparator() + catalog;
        messages.add(Map.of("role", "system", "content", prompt));
        if (request.getHistory() != null) {
            int start = Math.max(0, request.getHistory().size() - 12);
            request.getHistory().subList(start, request.getHistory().size()).stream()
                    .filter(item -> item != null && ("user".equals(item.getRole()) || "assistant".equals(item.getRole())))
                    .filter(item -> item.getContent() != null && !item.getContent().isBlank())
                    .forEach(item -> messages.add(Map.of("role", item.getRole(), "content", item.getContent().substring(0, Math.min(1500, item.getContent().length())))));
        }
        messages.add(Map.of("role", "user", "content", request.getMessage().substring(0, Math.min(2000, request.getMessage().length()))));
        String body = objectMapper.writeValueAsString(Map.of("model", groqModel, "messages", messages, "temperature", 0.5, "max_tokens", 600));
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .timeout(Duration.ofSeconds(25)).header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("Groq request failed");
        JsonNode content = objectMapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank()) throw new IllegalStateException("Groq returned an empty answer");
        return content.asText();
    }

    private String fallbackAdvice(String message, Long productId, ChatbotResponse search) {
        String normalized = normalize(message);
        List<ProductSearchResponse> products = search.getProducts() == null ? List.of() : search.getProducts();
        if (!products.isEmpty()) {
            String intro = search.getFilters() != null && search.getFilters().getCategory() != null
                    ? "Trong hệ thống có " + products.size() + " mẫu còn hàng thuộc nhóm bạn hỏi: "
                    : "Mình tìm thấy các sản phẩm phù hợp: ";
            return intro + products.stream().limit(8).map(product -> String.format("%s (%s), giá %s, chất liệu %s, màu %s, size %s",
                    product.getName(), safe(product.getBrandName()), product.getSalePrice() != null ? product.getSalePrice() : product.getBasePrice(),
                    safe(product.getMaterial()), safe(product.getColors()), safe(product.getSizes())))
                    .reduce((left, right) -> left + "; " + right).orElse("") + ".";
        }
        if (containsAny(normalized, "outfit", "phoi do", "set do", "di lam", "cong so", "mac gi")) {
            return "Mình chưa tìm thấy sản phẩm phù hợp để ghép set đồ đi làm. Bạn thử cho biết ngân sách hoặc muốn phối áo, quần hay váy nhé.";
        }
        if (containsAny(normalized, "chat lieu", "material", "vai gi")) return "Tell me the product name or open its details page so I can check its catalog material.";
        if (containsAny(normalized, "voucher", "ma giam", "khuyen mai")) return "Bạn nhập mã ở giỏ hàng hoặc checkout để hệ thống kiểm tra hạn dùng và điều kiện đơn hàng.";
        if (containsAny(normalized, "thanh toan", "chuyen khoan", "qr", "vnpay", "cod")) return "Checkout hỗ trợ COD, chuyển khoản và VNPay Sandbox. QR chuyển khoản cần cửa hàng cấu hình tài khoản nhận tiền.";
        if (containsAny(normalized, "don hang", "giao hang", "van chuyen", "tracking", "theo doi")) return "Bạn mở mục Đơn hàng để xem trạng thái và vận chuyển của đơn trong tài khoản.";
        if (containsAny(normalized, "size", "kich co", "vong nguc", "vong eo", "vong mong", "can nang", "chieu cao")) return "Bạn gửi tên mẫu cùng chiều cao, cân nặng và số đo ngực/eo/mông nếu có; mình sẽ đối chiếu với size đang có của mẫu đó.";
        if (productId != null) return "Bạn muốn biết giá, chất liệu, màu, size hay cách phối của sản phẩm đang xem?";
        if (containsAny(normalized, "he thong", "website", "ung dung", "ho tro duoc gi", "ban lam duoc gi", "giup duoc gi", "co the giup", "chuc nang", "tro ly")) return "Mình hỗ trợ tìm và so sánh catalog, tư vấn size/outfit, giải thích voucher và hướng dẫn thanh toán, giỏ hàng, đơn hàng. Bạn có thể hỏi như ‘các áo còn hàng’, ‘áo đen size M’ hoặc ‘set đồ đi làm dưới 1 triệu’.";
        return "Mình chưa tìm thấy sản phẩm phù hợp rõ với câu hỏi này. Bạn thử nêu loại đồ, màu, size hoặc ngân sách nhé.";
    }

    private String normalize(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value.toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace((char) 273, 'd');
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) if (text.contains(term)) return true;
        return false;
    }

    private String safe(Object value) {
        return value == null ? "chưa cập nhật" : value.toString();
    }
}
