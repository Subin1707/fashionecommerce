package com.fashion.smartsize.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmartSizeRequest {
    private Double height;
    private Double weight;
    private Double chest;
    private Double waist;
    private Double hip;
    private Integer age;
    private String bodyMeasurements;
    private String fitPreference;

    public SmartSizeRequest() {
    }
    public SmartSizeRequest(final Double height, final Double weight, final Double chest, final Double waist, final Double hip, final Integer age, final String bodyMeasurements, final String fitPreference) {
        this.height = height;
        this.weight = weight;
        this.chest = chest;
        this.waist = waist;
        this.hip = hip;
        this.age = age;
        this.bodyMeasurements = bodyMeasurements;
        this.fitPreference = fitPreference;
    }
}
