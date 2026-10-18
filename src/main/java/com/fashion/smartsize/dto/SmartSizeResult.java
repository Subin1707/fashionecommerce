package com.fashion.smartsize.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SmartSizeResult {
    private String recommendedSize;
    private String confidence;
    private String reasoning;

    public SmartSizeResult() {
    }
    public SmartSizeResult(final String recommendedSize, final String confidence, final String reasoning) {
        this.recommendedSize = recommendedSize;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }
}
