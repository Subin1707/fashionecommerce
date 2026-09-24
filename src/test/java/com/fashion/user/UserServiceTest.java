package com.fashion.user;

import com.fashion.user.entity.Role;
import com.fashion.user.entity.User;
import com.fashion.user.dto.CreateUserRequest;
import com.fashion.user.repository.RoleRepository;
import com.fashion.user.repository.UserRepository;
import com.fashion.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateCustomerUser() {
        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(new Role("CUSTOMER")));

        CreateUserRequest request = CreateUserRequest.builder()
            .fullName("Nguyen Van A")
            .email("customer@example.com")
            .password("123456")
            .phone("0909123456")
            .address("Hanoi")
            .role(customerRole.getName())
            .status("ACTIVE")
            .build();

        User saved = userService.createUser(request);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole().getName()).isEqualTo("CUSTOMER");
        assertThat(saved.getPassword()).isNotEqualTo("123456");
        assertThat(passwordEncoder.matches("123456", saved.getPassword())).isTrue();
        assertThat(userRepository.findByEmail("customer@example.com")).isPresent();
    }
}
