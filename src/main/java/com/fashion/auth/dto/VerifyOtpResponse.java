package com.fashion.auth.dto;

public record VerifyOtpResponse(
        boolean verified,
        boolean locked,
        long retryAfterSeconds,
        int attemptsRemaining,
        String message) {
}
