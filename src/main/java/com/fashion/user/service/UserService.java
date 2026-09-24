package com.fashion.user.service;

import com.fashion.user.entity.Role;
import com.fashion.user.entity.User;
import com.fashion.user.dto.CreateUserRequest;
import com.fashion.user.dto.UpdateProfileRequest;
import com.fashion.user.repository.RoleRepository;
import com.fashion.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll(PageRequest.of(0, 10, Sort.by("id").ascending())).getContent();
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        if (request == null || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không hợp lệ");
        }

        String roleName = request.getRole() == null || request.getRole().isBlank()
                ? "CUSTOMER" : normalizeRoleName(request.getRole());
        Role role = findAllowedRole(roleName);

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .avatar(request.getAvatar())
                .role(role)
                .status(request.getStatus() == null ? "ACTIVE" : request.getStatus())
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User saveUser(User user) {
        if (user.getRole() == null || user.getRole().getId() == null) {
            user.setRole(roleRepository.findByName("CUSTOMER")
                    .orElseGet(() -> roleRepository.save(new Role("CUSTOMER"))));
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User existing = getUserById(id);
        existing.setFullName(updatedUser.getFullName());
        existing.setEmail(updatedUser.getEmail());
        existing.setPhone(updatedUser.getPhone());
        existing.setAddress(updatedUser.getAddress());
        existing.setAvatar(updatedUser.getAvatar());
        existing.setStatus(updatedUser.getStatus());
        if (updatedUser.getRole() != null) {
            Role role = findAllowedRole(updatedUser.getRole().getName());
            existing.setRole(role);
        }
        return userRepository.save(existing);
    }

    @Transactional
    public User updateProfile(Long id, UpdateProfileRequest request) {
        User existing = getUserById(id);
        if (request == null) {
            return existing;
        }
        if (request.getFullName() != null) {
            existing.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            existing.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            existing.setAddress(request.getAddress());
        }
        if (request.getAvatar() != null) {
            existing.setAvatar(request.getAvatar());
        }
        return userRepository.save(existing);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private Role findAllowedRole(String roleName) {
        String normalizedRoleName = normalizeRoleName(roleName);
        if (!normalizedRoleName.equals("ADMIN") && !normalizedRoleName.equals("CUSTOMER")) {
            throw new IllegalArgumentException("Role không được phép");
        }
        return roleRepository.findByName(normalizedRoleName)
                .orElseGet(() -> roleRepository.save(new Role(normalizedRoleName)));
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role không hợp lệ");
        }
        String normalized = roleName.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }

}
