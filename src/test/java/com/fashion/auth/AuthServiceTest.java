package com.fashion.auth;

import com.fashion.auth.dto.LoginRequest;
import com.fashion.auth.dto.LoginResponse;
import com.fashion.auth.dto.RegisterRequest;
import com.fashion.auth.service.AuthService;
import com.fashion.security.CustomUserDetailsService;
import com.fashion.user.entity.Role;
import com.fashion.user.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldRegisterAndLoginCustomer() {
        roleRepository.findByName("CUSTOMER").orElseGet(() -> roleRepository.save(new Role("CUSTOMER")));
        roleRepository.findByName("ADMIN").orElseGet(() -> roleRepository.save(new Role("ADMIN")));

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFullName("Alice Customer");
        registerRequest.setEmail("alice@example.com");
        registerRequest.setPassword("123456");
        registerRequest.setPhone("0901000001");
        registerRequest.setAddress("Hanoi");

        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("alice@example.com");
        loginRequest.setPassword("123456");

        LoginResponse loginResponse = authService.login(loginRequest);

        assertThat(loginResponse.getToken()).isNotBlank();
        assertThat(loginResponse.getRole()).isEqualTo("CUSTOMER");
        assertThat(customUserDetailsService.loadUserByUsername("alice@example.com")
            .getAuthorities())
            .extracting(Object::toString)
            .containsExactly("ROLE_CUSTOMER");
    }
}
