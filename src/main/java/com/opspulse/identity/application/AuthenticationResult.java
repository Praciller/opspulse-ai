package com.opspulse.identity.application;

import com.opspulse.identity.domain.User;

public record AuthenticationResult(
        String accessToken,
        String refreshToken,
        long expiresIn,
        User user) {}
