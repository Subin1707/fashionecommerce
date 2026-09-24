package com.fashion.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashion.auth.dto.RegisterRequest;
import com.fashion.auth.service.AuthService;
import com.fashion.security.JwtService;
import com.fashion.user.entity.Role;
import com.fashion.user.entity.User;
import com.fashion.user.repository.RoleRepository;
import com.fashion.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @InjectMocks AuthService authService;

    @Test
    void registerCreatesActiveCustomerWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test Customer");
        request.setEmail("test@example.com");
        request.setPassword("plain-password");
        Role role = new Role("CUSTOMER");
        User saved = User.builder().email(request.getEmail()).role(role).status("ACTIVE").build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = authService.register(request);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getRole().getName()).isEqualTo("CUSTOMER");
        verify(passwordEncoder).encode("plain-password");
    }
}
