package com.fashion.voucher.config;

import com.fashion.voucher.entity.Voucher;
import com.fashion.voucher.repository.VoucherRepository;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
@Order(1)
public class VoucherInitializer implements CommandLineRunner {

    private final VoucherRepository voucherRepository;

    public VoucherInitializer(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (voucherRepository.findByCode("FASHION10").isPresent()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        voucherRepository.save(Voucher.builder()
                .code("FASHION10")
                .discountType("PERCENTAGE")
                .discountValue(10.0)
                .minOrderValue(200000.0)
                .maxDiscount(0.0)
                .startDate(now.minusYears(1))
                .endDate(now.plusYears(10))
                .status("ACTIVE")
                .build());
    }
}
