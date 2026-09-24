package com.fashion.smartsize.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartSizeResult {
    private String recommendedSize;
    private String confidence;
    private String reasoning;
}
