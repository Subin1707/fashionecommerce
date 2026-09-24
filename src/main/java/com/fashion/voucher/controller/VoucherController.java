package com.fashion.voucher.controller;

import com.fashion.voucher.dto.VoucherRequest;
import com.fashion.voucher.dto.VoucherValidationResult;
import com.fashion.voucher.entity.Voucher;
import com.fashion.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<Voucher>> getAllVouchers() {
        return ResponseEntity.ok(voucherService.getAllVouchers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Voucher> getVoucherById(@PathVariable Long id) {
        return ResponseEntity.ok(voucherService.getVoucherById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Voucher> createVoucher(@RequestBody VoucherRequest request) {
        return ResponseEntity.ok(voucherService.createVoucher(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Voucher> updateVoucher(@PathVariable Long id, @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(voucherService.updateVoucher(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<VoucherValidationResult> validateVoucher(@RequestParam String code, @RequestParam Double orderValue) {
        return ResponseEntity.ok(voucherService.validateVoucher(code, orderValue));
    }
}
