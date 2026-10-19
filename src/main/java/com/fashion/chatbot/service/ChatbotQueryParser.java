package com.fashion.chatbot.service;

import com.fashion.chatbot.dto.ChatbotFilters;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ChatbotQueryParser {
    private static final Pattern SIZE = Pattern.compile("\\b(xxl|xl|xs|s|m|l)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE = Pattern.compile("(?:duoi|toi da|ngan sach|budget|<)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|nghin dong|nghin|000|k|tr)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Set<String> PRODUCT_NOUNS = Set.of("ao", "vay", "dam", "quan", "giay", "tui", "vi");
    private static final Set<String> IGNORE = Set.of(
            "toi", "muon", "tim", "kiem", "xem", "cho", "minh", "ban", "giup", "voi", "mot", "cac", "nhung",
            "la", "gi", "nao", "nhi", "nhe", "nay", "kia", "co", "khong", "dang", "duoc", "ve", "tu", "van",
            "goi", "phoi", "outfit", "style", "phong", "cach", "mac", "di", "lam", "san", "pham", "thong", "tin",
            "gia", "bao", "nhieu", "chat", "lieu", "con", "hang", "size", "kich", "mau", "sac", "hop", "duoi", "da",
            "ngan", "sach", "trieu", "nghin", "dong", "vnd", "k", "he", "cua", "shop", "website", "ung", "dung",
            "tinh", "nang", "ho", "tro", "thanh", "toan", "don", "order", "voucher", "ma", "khuyen", "mai", "giao",
            "chuyen", "ship", "mua", "hien", "trong", "tat", "ca");

    public ChatbotFilters parse(String input) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Search text cannot be blank");
        String normalized = normalize(input);
        return ChatbotFilters.builder().keyword(extractKeyword(input)).category(detectCategory(normalized))
                .style(normalized.contains("di lam") || normalized.contains("cong so") || normalized.contains("office") || normalized.contains("formal") ? "OFFICE" : null)
                .color(normalized.contains("trung tinh") || normalized.contains("neutral") ? "NEUTRAL" : extractColor(normalized))
                .size(extractSize(normalized)).maxPrice(extractMaxPrice(normalized)).build();
    }

    private String detectCategory(String input) {
        if (input.matches(".*\\bao\\b.*") || input.matches(".*\\b(shirt|shirts|tee|top)\\b.*")) return "APPAREL_TOP";
        if (input.matches(".*\\b(quan|jeans|trousers|pants)\\b.*")) return "BOTTOM";
        if (input.contains("dam") || input.matches(".*\\b(dress|dresses)\\b.*")) return "DRESS";
        if (input.matches(".*\\bvay\\b.*")) return "DRESS";
        if (input.matches(".*\\b(giay|sneaker|shoes)\\b.*")) return "SHOES";
        if (input.matches(".*\\b(tui|bag|bags)\\b.*")) return "BAGS";
        return null;
    }

    private String extractSize(String input) {
        Matcher matcher = SIZE.matcher(input);
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : null;
    }

    private BigDecimal extractMaxPrice(String input) {
        Matcher matcher = PRICE.matcher(input);
        if (!matcher.find()) return null;
        BigDecimal value = new BigDecimal(matcher.group(1).replace(',', '.'));
        String unit = matcher.group(2);
        if (unit == null) return value;
        if (unit.equalsIgnoreCase("tr") || unit.equalsIgnoreCase("trieu")) return value.multiply(BigDecimal.valueOf(1_000_000));
        if (unit.equalsIgnoreCase("k") || unit.equalsIgnoreCase("000") || unit.toLowerCase(Locale.ROOT).startsWith("nghin")) return value.multiply(BigDecimal.valueOf(1000));
        return value;
    }

    private String extractKeyword(String input) {
        Matcher matcher = WORD.matcher(input);
        String best = null;
        int bestScore = 0;
        while (matcher.find()) {
            String original = matcher.group();
            String normalized = normalize(original);
            if (PRODUCT_NOUNS.contains(normalized)) return original;
            if (IGNORE.contains(normalized) || normalized.matches("\\d+(?:[.,]\\d+)?") || normalized.length() < 3) continue;
            int score = normalized.length();
            if (Set.of("cotton", "linen", "jeans", "denim", "bomber", "hoodie", "dress").contains(normalized)) score += 5;
            if (score > bestScore) { best = original; bestScore = score; }
        }
        return best;
    }

    private String extractColor(String input) {
        if (input.contains("den") || input.contains("black")) return "black";
        if (input.contains("trang") || input.contains("white")) return "white";
        if (input.contains("xanh") || input.contains("blue")) return "blue";
        if (input.contains("do") || input.contains("red")) return "red";
        return null;
    }

    private String normalize(String input) {
        return Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace((char) 273, 'd');
    }
}