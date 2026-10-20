package com.fashion.auth.dto;

public record ResetPasswordRequest(String email, String code, String newPassword) {
}
