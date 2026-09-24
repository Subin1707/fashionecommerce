package com.fashion.voucher.service;

import com.fashion.voucher.dto.VoucherRequest;
import com.fashion.voucher.dto.VoucherValidationResult;
import com.fashion.voucher.entity.Voucher;
import com.fashion.voucher.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;

    @Transactional
    public Voucher createVoucher(VoucherRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu voucher không hợp lệ");
        }

        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Mã voucher không hợp lệ");
        }

        if (voucherRepository.findByCode(request.getCode().trim()).isPresent()) {
            throw new IllegalArgumentException("Mã voucher đã tồn tại");
        }

        String type = request.getDiscountType();
        if ("PERCENT".equalsIgnoreCase(type)) type = "PERCENTAGE";
        if ("FIXED".equalsIgnoreCase(type)) type = "FIXED_AMOUNT";

        if (type == null || (!"PERCENTAGE".equalsIgnoreCase(type) && !"FIXED_AMOUNT".equalsIgnoreCase(type))) {
            throw new IllegalArgumentException("Loại giảm giá không hợp lệ");
        }

        if (request.getDiscountValue() == null || request.getDiscountValue() <= 0) {
            throw new IllegalArgumentException("Giá trị giảm giá không hợp lệ");
        }

        Voucher voucher = Voucher.builder()
                .code(request.getCode().trim().toUpperCase())
                .discountType(type.toUpperCase())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue() == null ? 0.0 : request.getMinOrderValue())
                .maxDiscount(request.getMaxDiscount() == null ? 0.0 : request.getMaxDiscount())
                .startDate(request.getStartDate() == null ? LocalDateTime.now() : request.getStartDate())
                .endDate(request.getEndDate() == null ? LocalDateTime.now().plusDays(30) : request.getEndDate())
                .status(request.getStatus() == null ? "ACTIVE" : request.getStatus().toUpperCase())
                .build();

        return voucherRepository.save(voucher);
    }

    @Transactional(readOnly = true)
    public java.util.List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Voucher getVoucherById(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại với ID: " + id));
    }

    @Transactional
    public Voucher updateVoucher(Long id, VoucherRequest request) {
        Voucher existing = getVoucherById(id);

        if (request.getCode() != null && !request.getCode().isBlank()) {
            String newCode = request.getCode().trim().toUpperCase();
            if (!newCode.equals(existing.getCode()) && voucherRepository.findByCode(newCode).isPresent()) {
                throw new IllegalArgumentException("Mã voucher đã tồn tại");
            }
            existing.setCode(newCode);
        }

        if (request.getDiscountType() != null && !request.getDiscountType().isBlank()) {
            String type = request.getDiscountType();
            if ("PERCENT".equalsIgnoreCase(type)) type = "PERCENTAGE";
            if ("FIXED".equalsIgnoreCase(type)) type = "FIXED_AMOUNT";
            existing.setDiscountType(type.toUpperCase());
        }

        if (request.getDiscountValue() != null && request.getDiscountValue() > 0) {
            existing.setDiscountValue(request.getDiscountValue());
        }

        if (request.getMinOrderValue() != null) {
            existing.setMinOrderValue(request.getMinOrderValue());
        }

        if (request.getMaxDiscount() != null) {
            existing.setMaxDiscount(request.getMaxDiscount());
        }

        if (request.getStartDate() != null) {
            existing.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            existing.setEndDate(request.getEndDate());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            existing.setStatus(request.getStatus().toUpperCase());
        }

        return voucherRepository.save(existing);
    }

    @Transactional
    public void deleteVoucher(Long id) {
        if (!voucherRepository.existsById(id)) {
            throw new IllegalArgumentException("Voucher không tồn tại với ID: " + id);
        }
        voucherRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public VoucherValidationResult validateVoucher(String code, Double orderValue) {
        if (code == null || code.isBlank()) {
            return VoucherValidationResult.builder().valid(false).message("Mã voucher không hợp lệ").discountAmount(0).finalAmount(orderValue == null ? 0 : orderValue).build();
        }

        Voucher voucher = voucherRepository.findByCode(code.trim().toUpperCase())
            .orElse(null);

        if (voucher == null) {
            return VoucherValidationResult.builder().valid(false).code(code.trim().toUpperCase())
                .message("Voucher không tồn tại").discountAmount(0).finalAmount(orderValue == null ? 0 : orderValue).build();
        }

        LocalDateTime now = LocalDateTime.now();
        if (!"ACTIVE".equalsIgnoreCase(voucher.getStatus())) {
            return VoucherValidationResult.builder().valid(false).code(voucher.getCode()).message("Voucher không còn hiệu lực").discountAmount(0).finalAmount(orderValue == null ? 0 : orderValue).build();
        }

        if (voucher.getStartDate() == null || voucher.getEndDate() == null
                || now.isBefore(voucher.getStartDate()) || now.isAfter(voucher.getEndDate())) {
            return VoucherValidationResult.builder().valid(false).code(voucher.getCode()).message("Voucher đã hết hạn").discountAmount(0).finalAmount(orderValue == null ? 0 : orderValue).build();
        }

        double baseAmount = orderValue == null ? 0 : orderValue;
        double minimumOrderValue = voucher.getMinOrderValue() == null ? 0.0 : voucher.getMinOrderValue();
        double discountValue = voucher.getDiscountValue() == null ? 0.0 : voucher.getDiscountValue();
        if (discountValue <= 0) {
            return VoucherValidationResult.builder().valid(false).code(voucher.getCode()).message("Voucher chưa có giá trị giảm hợp lệ").discountAmount(0).finalAmount(baseAmount).build();
        }
        if (baseAmount < minimumOrderValue) {
            return VoucherValidationResult.builder().valid(false).code(voucher.getCode()).message("Đơn hàng chưa đủ điều kiện").discountAmount(0).finalAmount(baseAmount).build();
        }

        double discountAmount;
        if ("PERCENTAGE".equalsIgnoreCase(voucher.getDiscountType())) {
            discountAmount = baseAmount * (discountValue / 100.0);
        } else {
            discountAmount = discountValue;
        }

        if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount() > 0 && discountAmount > voucher.getMaxDiscount()) {
            discountAmount = voucher.getMaxDiscount();
        }

        double finalAmount = Math.max(0, baseAmount - discountAmount);

        return VoucherValidationResult.builder()
                .valid(true)
                .code(voucher.getCode())
                .message("Áp dụng voucher thành công")
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }
}
