package com.fashion.admin.controller;

import com.fashion.user.entity.User;
import com.fashion.user.dto.UserResponse;
import com.fashion.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    @GetMapping("/customers")
    public ResponseEntity<List<UserResponse>> getAllCustomers() {
        return ResponseEntity.ok(userService.getAllUsers().stream()
                .filter(user -> user.getRole() != null && "CUSTOMER".equalsIgnoreCase(user.getRole().getName()))
                .map(UserResponse::fromEntity)
                .toList());
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<UserResponse> getCustomerById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user.getRole() == null || !"CUSTOMER".equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalArgumentException("User không phải customer");
        }
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PutMapping("/customers/{id}/lock")
    public ResponseEntity<UserResponse> lockCustomer(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user.getRole() == null || !"CUSTOMER".equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalArgumentException("Chỉ có thể khóa customer");
        }
        user.setStatus("LOCKED");
        return ResponseEntity.ok(UserResponse.fromEntity(userService.saveUser(user)));
    }

    @PutMapping("/customers/{id}/unlock")
    public ResponseEntity<UserResponse> unlockCustomer(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user.getRole() == null || !"CUSTOMER".equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalArgumentException("Chỉ có thể mở khóa customer");
        }
        user.setStatus("ACTIVE");
        return ResponseEntity.ok(UserResponse.fromEntity(userService.saveUser(user)));
    }

}
