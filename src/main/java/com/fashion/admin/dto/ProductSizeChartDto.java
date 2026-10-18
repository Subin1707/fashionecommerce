package com.fashion.admin.dto;
import lombok.Builder;
import lombok.Data;
@Data
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

    public ProductSizeChartDto() {
    }
    public ProductSizeChartDto(final Long id, final String sizeLabel, final Double minChest, final Double maxChest, final Double minWaist, final Double maxWaist, final Double minHip, final Double maxHip) {
        this.id = id;
        this.sizeLabel = sizeLabel;
        this.minChest = minChest;
        this.maxChest = maxChest;
        this.minWaist = minWaist;
        this.maxWaist = maxWaist;
        this.minHip = minHip;
        this.maxHip = maxHip;
    }
}
