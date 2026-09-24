package com.fashion.smartsize.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SmartSizeRequest {
    private Double height;
    private Double weight;
    private Double chest;
    private Double waist;
    private Double hip;
    private Integer age;
    private String bodyMeasurements;
    private String fitPreference;
}
