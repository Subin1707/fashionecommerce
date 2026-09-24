package com.fashion.smartsize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fashion.product.repository.ProductRepository;
import com.fashion.smartsize.dto.SmartSizeRequest;
import com.fashion.smartsize.service.SmartSizeService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SmartSizeUnitTest {

    @Mock ProductRepository productRepository;
    @InjectMocks SmartSizeService smartSizeService;

    @Test
    void recommendsMediumForMediumProfile() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(new com.fashion.product.entity.Product()));
        SmartSizeRequest request = new SmartSizeRequest();
        request.setHeight(168.0);
        request.setWeight(60.0);
        request.setChest(94.0);
        request.setWaist(76.0);
        request.setHip(98.0);

        var result = smartSizeService.recommendSize(100L, request);

        assertThat(result.getRecommendedSize()).isEqualTo("M");
        assertThat(result.getConfidence()).endsWith("%");
    }

    @Test
    void rejectsIncompleteMeasurements() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(new com.fashion.product.entity.Product()));

        assertThatThrownBy(() -> smartSizeService.recommendSize(100L, new SmartSizeRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Height");
    }
}
