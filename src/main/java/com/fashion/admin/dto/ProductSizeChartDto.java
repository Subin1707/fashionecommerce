package com.fashion.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSizeChartDto {
    private Long id;
    private String sizeLabel;
    private Double minChest;
    private Double maxChest;
    private Double minWaist;
    private Double maxWaist;
    private Double minHip;
    private Double maxHip;
}
