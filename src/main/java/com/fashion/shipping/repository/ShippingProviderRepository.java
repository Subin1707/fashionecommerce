package com.fashion.shipping.repository;

import com.fashion.shipping.entity.ShippingProvider;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingProviderRepository extends JpaRepository<ShippingProvider, Long> {
    List<ShippingProvider> findByStatusOrderByNameAsc(String status);
}