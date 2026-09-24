package com.fashion.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashion.chatbot.dto.ChatbotFilters;
import com.fashion.chatbot.service.ChatbotQueryParser;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ChatbotQueryParserTest {

    private final ChatbotQueryParser parser = new ChatbotQueryParser();

    @Test
    void parsesVietnameseOfficeTopRequest() {
        ChatbotFilters filters = parser.parse("Cho tôi áo đi làm màu trung tính size M dưới 300k");

        assertThat(filters.getCategory()).isEqualTo("TOP");
        assertThat(filters.getStyle()).isEqualTo("OFFICE");
        assertThat(filters.getColor()).isEqualTo("NEUTRAL");
        assertThat(filters.getSize()).isEqualTo("M");
        assertThat(filters.getMaxPrice()).isEqualByComparingTo(BigDecimal.valueOf(300000));
    }

    @Test
    void parsesExplicitColorAndPriceWithoutUnit() {
        ChatbotFilters filters = parser.parse("áo màu đen size L dưới 250000");

        assertThat(filters.getCategory()).isEqualTo("TOP");
        assertThat(filters.getColor()).isEqualTo("BLACK");
        assertThat(filters.getSize()).isEqualTo("L");
        assertThat(filters.getMaxPrice()).isEqualByComparingTo(BigDecimal.valueOf(250000));
    }
}
