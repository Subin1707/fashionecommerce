package com.fashion.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashion.auth.dto.RegisterRequest;
import com.fashion.auth.entity.EmailOtp;
import com.fashion.auth.repository.EmailOtpRepository;
import com.fashion.auth.service.AuthService;
import com.fashion.security.JwtService;
import com.fashion.user.entity.Role;
import com.fashion.user.entity.User;
import com.fashion.user.repository.RoleRepository;
import com.fashion.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock EmailOtpRepository emailOtpRepository;
    @Mock JavaMailSender mailSender;
    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService,
                authenticationManager, emailOtpRepository, mailSender, "noreply@example.com");
    }

    @Test
    void registerCreatesPendingCustomerAndSendsOtp() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test Customer");
        request.setEmail("test@example.com");
        request.setPassword("plain-password");
        Role role = new Role("CUSTOMER");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(role));
        when(emailOtpRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User pendingUser = userCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(pendingUser.getStatus()).isEqualTo("PENDING");
        org.assertj.core.api.Assertions.assertThat(pendingUser.getRole().getName()).isEqualTo("CUSTOMER");
        org.assertj.core.api.Assertions.assertThat(pendingUser.getPassword()).isEqualTo("encoded");
        verify(emailOtpRepository).save(any(EmailOtp.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(passwordEncoder).encode("plain-password");
    }
}
