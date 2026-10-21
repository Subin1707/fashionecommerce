package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthMessageResponse;
import com.fashion.auth.dto.LoginRequest;
import com.fashion.auth.dto.LoginResponse;
import com.fashion.auth.dto.RegisterRequest;
import com.fashion.auth.dto.VerifyOtpRequest;
import com.fashion.auth.dto.VerifyOtpResponse;
import com.fashion.auth.dto.ForgotPasswordRequest;
import com.fashion.auth.dto.ResetPasswordRequest;
import com.fashion.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthMessageResponse> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(new AuthMessageResponse(
                "OTP sent to email",
                request.getEmail(),
                "CUSTOMER"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthMessageResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(new AuthMessageResponse(
                "Nếu email thuộc tài khoản đang hoạt động, mã xác thực sẽ được gửi đến hộp thư đó.",
                request.email(),
                null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthMessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (!authService.resetPassword(request)) {
            throw new IllegalArgumentException("Mã xác thực không đúng, đã hết hạn hoặc vượt quá số lần thử.");
        }
        return ResponseEntity.ok(new AuthMessageResponse(
                "Mật khẩu đã được thay đổi. Bạn có thể đăng nhập bằng mật khẩu mới.",
                request.email(),
                null));
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthMessageResponse> logout() {
        return ResponseEntity.ok(new AuthMessageResponse("Logged out successfully", null, null));
    }
}
