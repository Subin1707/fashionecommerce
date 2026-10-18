package com.fashion.style.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StyleMatchRequest {
    private String color;
    private String style;
    private String category;
    private String pattern;
    private String fashionType;
    private String imageUrl;

    public StyleMatchRequest() {
    }
    public StyleMatchRequest(final String color, final String style, final String category, final String pattern, final String fashionType, final String imageUrl) {
        this.color = color;
        this.style = style;
        this.category = category;
        this.pattern = pattern;
        this.fashionType = fashionType;
        this.imageUrl = imageUrl;
    }
}
