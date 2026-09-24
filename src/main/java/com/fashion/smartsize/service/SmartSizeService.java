package com.fashion.smartsize.service;

import com.fashion.product.repository.ProductRepository;
import com.fashion.smartsize.dto.SmartSizeRequest;
import com.fashion.smartsize.dto.SmartSizeResult;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmartSizeService {

    private static final Map<String, SizeRange> SIZE_CHART = new LinkedHashMap<>();

    static {
        SIZE_CHART.put("S", new SizeRange(84, 90, 68, 74, 88, 94));
        SIZE_CHART.put("M", new SizeRange(90, 96, 74, 80, 94, 100));
        SIZE_CHART.put("L", new SizeRange(96, 104, 80, 88, 100, 108));
        SIZE_CHART.put("XL", new SizeRange(104, 112, 88, 98, 108, 118));
        SIZE_CHART.put("XXL", new SizeRange(112, 120, 98, 108, 118, 128));
    }

    private final ProductRepository productRepository;

    public SmartSizeResult recommendSize(Long productId, SmartSizeRequest request) {
        productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("San pham khong ton tai"));

        if (request == null) {
            throw new IllegalArgumentException("Thong tin size khong hop le");
        }

        validateRequest(request);
        Recommendation recommendation = estimateByBody(request);

        return SmartSizeResult.builder()
                .recommendedSize(recommendation.size())
                .confidence(recommendation.confidence() + "%")
                .reasoning(recommendation.explanation())
                .build();
    }

    private Recommendation estimateByBody(SmartSizeRequest request) {
        String size = calculateRecommendedSize(request.getChest(), request.getWaist(), request.getHip());
        int confidence = calculateConfidence(size, request.getChest(), request.getWaist(), request.getHip());
        String explanation = buildExplanation(size, request.getChest(), request.getWaist(), request.getHip());

        if (suspiciousRecommendation(size, request.getHeight(), request.getWeight())) {
            explanation += " Can kiem tra lai vi BMI cao nhung size de xuat nho.";
            confidence = Math.min(confidence, 50);
        }

        return new Recommendation(size, confidence, explanation);
    }

    private String calculateRecommendedSize(double chest, double waist, double hip) {
        String bestSize = null;
        double bestScore = Double.MAX_VALUE;

        for (Map.Entry<String, SizeRange> entry : SIZE_CHART.entrySet()) {
            SizeRange range = entry.getValue();

            double score = 0;
            score += rangePenalty(chest, range.minChest, range.maxChest) * 0.45;
            score += rangePenalty(waist, range.minWaist, range.maxWaist) * 0.30;
            score += rangePenalty(hip, range.minHip, range.maxHip) * 0.25;

            score += tightPenalty(chest, range.maxChest) * 2.0;
            score += tightPenalty(waist, range.maxWaist) * 1.5;
            score += tightPenalty(hip, range.maxHip) * 1.5;

            if (score < bestScore) {
                bestScore = score;
                bestSize = entry.getKey();
            }
        }

        return bestSize;
    }

    private double rangePenalty(double value, double min, double max) {
        if (value >= min && value <= max) {
            return 0;
        }

        if (value < min) {
            return (min - value) / (max - min);
        }

        return (value - max) / (max - min);
    }

    private double tightPenalty(double value, double max) {
        if (value <= max) {
            return 0;
        }

        return value - max;
    }

    private double calculateBMI(double heightCm, double weightKg) {
        double heightM = heightCm / 100.0;
        return weightKg / (heightM * heightM);
    }

    private boolean suspiciousRecommendation(String size, double height, double weight) {
        double bmi = calculateBMI(height, weight);
        return bmi >= 27 && "S".equals(size);
    }

    private int calculateConfidence(String size, double chest, double waist, double hip) {
        SizeRange range = SIZE_CHART.get(size);

        double penalty = 0;
        penalty += rangePenalty(chest, range.minChest, range.maxChest) * 45;
        penalty += rangePenalty(waist, range.minWaist, range.maxWaist) * 30;
        penalty += rangePenalty(hip, range.minHip, range.maxHip) * 25;

        int confidence = (int) Math.round(100 - penalty);
        return Math.max(0, Math.min(100, confidence));
    }

    private String buildExplanation(String size, double chest, double waist, double hip) {
        SizeRange range = SIZE_CHART.get(size);
        StringBuilder result = new StringBuilder();

        result.append("Goi y size ")
                .append(size)
                .append(". ");

        appendFit(result, "Vong nguc", chest, range.minChest, range.maxChest);
        appendFit(result, "Vong eo", waist, range.minWaist, range.maxWaist);
        appendFit(result, "Vong mong", hip, range.minHip, range.maxHip);

        return result.toString().trim();
    }

    private void appendFit(StringBuilder result, String label, double value, double min, double max) {
        if (value >= min && value <= max) {
            result.append(label).append(" phu hop. ");
            return;
        }

        if (value < min) {
            result.append(label).append(" nho hon khoang size. ");
            return;
        }

        result.append(label).append(" lon hon khoang size. ");
    }

    private void validateRequest(SmartSizeRequest request) {
        Double[] measurements = {request.getHeight(), request.getWeight(), request.getChest(),
                request.getWaist(), request.getHip()};
        if (Arrays.stream(measurements).anyMatch(value -> value == null || value <= 0)) {
            throw new IllegalArgumentException("Vui long nhap day du Height, Weight, Chest, Waist va Hip");
        }
    }

    private record Recommendation(String size, int confidence, String explanation) {
    }

    private static class SizeRange {
        double minChest;
        double maxChest;
        double minWaist;
        double maxWaist;
        double minHip;
        double maxHip;

        SizeRange(double minChest, double maxChest, double minWaist, double maxWaist,
                  double minHip, double maxHip) {
            this.minChest = minChest;
            this.maxChest = maxChest;
            this.minWaist = minWaist;
            this.maxWaist = maxWaist;
            this.minHip = minHip;
            this.maxHip = maxHip;
        }
    }
}
