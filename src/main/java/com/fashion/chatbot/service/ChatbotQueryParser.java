package com.fashion.chatbot.service;

import com.fashion.chatbot.dto.ChatbotFilters;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ChatbotQueryParser {

    private static final Pattern SIZE = Pattern.compile("\\b(xxl|xl|xs|s|m|l)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE = Pattern.compile(
            "(?:duoi|duoi gia|toi da|<)\\s*(\\d+(?:[.,]\\d+)?)\\s*(k|000|nghin|nghin dong)?",
            Pattern.CASE_INSENSITIVE);

    public ChatbotFilters parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Nội dung tìm kiếm không được để trống");
        }
        String normalized = normalize(input);
        return ChatbotFilters.builder()
                .keyword(null)
                .category(normalized.contains("ao") || normalized.contains("shirt") || normalized.contains("top") ? "TOP" : null)
                .style(normalized.contains("di lam") || normalized.contains("cong so")
                        || normalized.contains("office") || normalized.contains("formal") ? "OFFICE" : null)
                .color(normalized.contains("trung tinh") || normalized.contains("neutral")
                        ? "NEUTRAL" : extractColor(normalized))
                .size(extractSize(normalized))
                .maxPrice(extractMaxPrice(normalized))
                .build();
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
        if (unit != null && (unit.equalsIgnoreCase("k") || unit.startsWith("nghin"))) {
            value = value.multiply(BigDecimal.valueOf(1000));
        }
        return value;
    }

    private String extractColor(String input) {
        if (input.contains("den") || input.contains("black")) return "BLACK";
        if (input.contains("trang") || input.contains("white")) return "WHITE";
        if (input.contains("xanh") || input.contains("blue")) return "BLUE";
        if (input.contains("do") || input.contains("red")) return "RED";
        return null;
    }

    private String normalize(String input) {
        return Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd');
    }
}
