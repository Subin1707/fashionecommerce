package com.fashion.style.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StyleMatchRequest {
    private String color;
    private String style;
    private String category;
    private String pattern;
    private String fashionType;
    private String imageUrl;
}
