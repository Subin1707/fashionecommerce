package com.fashion.user.config;

import com.fashion.user.entity.Role;
import com.fashion.user.entity.User;
import com.fashion.user.repository.RoleRepository;
import com.fashion.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${fashion.admin.password:}")
    private String adminPassword;

    @Value("${fashion.customer.password:}")
    private String customerPassword;

    @Override
    public void run(String... args) {
        Role adminRole = ensureRole("ADMIN");
        Role customerRole = ensureRole("CUSTOMER");
        ensureAdmin(adminRole);
        ensureCustomer(customerRole);
    }

    private Role ensureRole(String name) {
        return roleRepository.findByName(name)
                .or(() -> roleRepository.findAll().stream()
                        .filter(role -> normalizeRoleName(role.getName()).equals(name))
                        .findFirst())
                .map(role -> {
                    if (!name.equals(role.getName())) {
                        role.setName(name);
                        return roleRepository.save(role);
                    }
                    return role;
                })
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private String normalizeRoleName(String name) {
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }

    private void ensureAdmin(Role adminRole) {
        if (adminPassword == null || adminPassword.isBlank()) {
            return;
        }
        userRepository.findByEmail("admin@fashion.local")
                .map(existingUser -> {
                    if (existingUser.getPassword() == null || existingUser.getPassword().isBlank()
                            || !passwordEncoder.matches(adminPassword, existingUser.getPassword())) {
                        existingUser.setPassword(passwordEncoder.encode(adminPassword));
                    }
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Fashion Admin")
                        .email("admin@fashion.local")
                        .password(passwordEncoder.encode(adminPassword))
                        .phone("0900000001")
                        .address("Fashion HQ")
                        .role(adminRole)
                        .status("ACTIVE")
                        .build()));
    }

    private void ensureCustomer(Role customerRole) {
        if (customerPassword == null || customerPassword.isBlank()) {
            return;
        }
        userRepository.findByEmail("customer@fashion.local")
                .map(existingUser -> {
                    if (existingUser.getPassword() == null || existingUser.getPassword().isBlank()
                            || !passwordEncoder.matches(customerPassword, existingUser.getPassword())) {
                        existingUser.setPassword(passwordEncoder.encode(customerPassword));
                    }
                    existingUser.setRole(customerRole);
                    existingUser.setStatus("ACTIVE");
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Test Customer")
                        .email("customer@fashion.local")
                        .password(passwordEncoder.encode(customerPassword))
                        .phone("0900000002")
                        .address("Test Address")
                        .role(customerRole)
                        .status("ACTIVE")
                        .build()));
    }
}
