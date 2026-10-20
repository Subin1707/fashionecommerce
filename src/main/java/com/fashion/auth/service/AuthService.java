package com.fashion.auth.service;

import com.fashion.auth.dto.LoginRequest;
import com.fashion.auth.dto.LoginResponse;
import com.fashion.auth.dto.RegisterRequest;
import com.fashion.auth.dto.VerifyOtpRequest;
import com.fashion.auth.dto.ResetPasswordRequest;
import com.fashion.auth.entity.EmailOtp;
import com.fashion.auth.repository.EmailOtpRepository;
import com.fashion.security.JwtService;
import com.fashion.user.entity.Role;
import com.fashion.user.entity.User;
import com.fashion.user.repository.RoleRepository;
import com.fashion.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import jakarta.mail.MessagingException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailOtpRepository emailOtpRepository;
    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       EmailOtpRepository emailOtpRepository,
                       JavaMailSender mailSender,
                       @Value("${spring.mail.username:}") String mailFrom) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailOtpRepository = emailOtpRepository;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = request.getPhone() == null ? "" : request.getPhone().trim();
        if (!phone.isBlank() && !phone.matches("\\d+")) {
            throw new IllegalArgumentException("Số điện thoại không được chứa chữ cái hoặc ký tự đặc biệt.");
        }
        if (!phone.isBlank() && !phone.matches("\\d{9,11}")) {
            throw new IllegalArgumentException("Số điện thoại phải gồm từ 9 đến 11 chữ số.");
        }
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null && "ACTIVE".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalArgumentException("Email n\u00e0y \u0111\u00e3 \u0111\u01b0\u1ee3c \u0111\u0103ng k\u00fd. H\u00e3y \u0111\u0103ng nh\u1eadp ho\u1eb7c d\u00f9ng email kh\u00e1c.");
        }
        if (!phone.isBlank() && userRepository.existsByPhoneAndEmailNot(phone, email)) {
            throw new IllegalArgumentException("S\u1ed1 \u0111i\u1ec7n tho\u1ea1i n\u00e0y \u0111\u00e3 \u0111\u01b0\u1ee3c s\u1eed d\u1ee5ng. Vui l\u00f2ng d\u00f9ng s\u1ed1 kh\u00e1c.");
        }

        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException("Email OTP is not configured. Set MAIL_USERNAME and MAIL_PASSWORD.");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();
        EmailOtp otp = emailOtpRepository.findByEmail(email).orElseGet(EmailOtp::new);
        if (otp.getLastSentAt() != null && otp.getLastSentAt().isAfter(now.minusSeconds(60))) {
            throw new IllegalArgumentException("Please wait before requesting another OTP");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(new Role("CUSTOMER")));

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = existing == null ? new User() : existing;
        user.setFullName(request.getFullName());
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setPhone(phone);
        user.setAddress(request.getAddress());
        user.setRole(customerRole);
        user.setStatus("PENDING");

        userRepository.save(user);
        otp.setEmail(email);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setExpiresAt(now.plusMinutes(10));
        otp.setLastSentAt(now);
        emailOtpRepository.save(otp);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("FASHION account verification code");
            message.setText("Your registration OTP is " + code + ". It expires in 10 minutes. Do not share this code.");
            mailSender.send(message);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not send OTP email. Check SMTP configuration.", exception);
        }
    }

    @Transactional
    public boolean verifyOtp(VerifyOtpRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        EmailOtp otp = emailOtpRepository.findByEmail(email)
                .orElse(null);
        if (otp == null || otp.getFailedAttempts() >= 5) {
            return false;
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            emailOtpRepository.delete(otp);
            return false;
        }
        if (request.code() == null || !request.code().matches("\\d{6}")
                || !passwordEncoder.matches(request.code(), otp.getCodeHash())) {
            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            if (otp.getFailedAttempts() >= 5) {
                emailOtpRepository.delete(otp);
            } else {
                emailOtpRepository.save(otp);
            }
            return false;
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Registration account not found"));
        user.setStatus("ACTIVE");
        userRepository.save(user);
        emailOtpRepository.delete(otp);
        return true;
    }

    @Transactional
    public void requestPasswordReset(String requestedEmail) {
        String email = normalizeEmail(requestedEmail);
        if (email.isBlank() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) return;
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) return;
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException("Chức năng gửi mã qua email chưa được cấu hình.");
        }

        LocalDateTime now = LocalDateTime.now();
        EmailOtp otp = emailOtpRepository.findByEmail(email).orElseGet(EmailOtp::new);
        if (otp.getLastSentAt() != null && otp.getLastSentAt().isAfter(now.minusSeconds(60))) return;

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        otp.setEmail(email);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setExpiresAt(now.plusMinutes(10));
        otp.setLastSentAt(now);
        otp.setFailedAttempts(0);
        emailOtpRepository.save(otp);

        try {
            var mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Mã xác thực đặt lại mật khẩu FASHION");
            helper.setText("Mã xác thực của bạn là: " + code + "\nMã có hiệu lực trong 10 phút. Không chia sẻ mã này với bất kỳ ai.");
            mailSender.send(mimeMessage);
        } catch (MessagingException | RuntimeException exception) {
            throw new IllegalStateException("Không thể gửi email. Vui lòng kiểm tra cấu hình hộp thư.", exception);
        }
    }

    @Transactional
    public boolean resetPassword(ResetPasswordRequest request) {
        String email = request == null ? "" : normalizeEmail(request.email());
        String code = request == null ? null : request.code();
        String newPassword = request == null ? null : request.newPassword();
        if (email.isBlank() || code == null || !code.matches("\\d{6}")) {
            throw new IllegalArgumentException("Email hoặc mã xác thực không hợp lệ.");
        }
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 100) {
            throw new IllegalArgumentException("Mật khẩu mới phải có từ 8 đến 100 ký tự.");
        }

        EmailOtp otp = emailOtpRepository.findByEmail(email).orElse(null);
        if (otp == null || otp.getFailedAttempts() >= 5) {
            return false;
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            emailOtpRepository.delete(otp);
            return false;
        }
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            if (otp.getFailedAttempts() >= 5) emailOtpRepository.delete(otp);
            else emailOtpRepository.save(otp);
            return false;
        }

        User user = userRepository.findByEmail(email)
                .filter(account -> "ACTIVE".equalsIgnoreCase(account.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản đang hoạt động cho email này."));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        emailOtpRepository.delete(otp);
        return true;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại. Vui lòng kiểm tra email hoặc đăng ký tài khoản mới."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Mật khẩu không đúng. Vui lòng thử lại.");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("Tài khoản chưa xác thực email. Vui lòng xác thực mã OTP trước khi đăng nhập.");
        }

        String roleName = user.getRole().getName().trim().toUpperCase(Locale.ROOT);
        String token = jwtService.generateToken(user.getEmail(), roleName);

        return LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(roleName)
                .build();
    }
}
