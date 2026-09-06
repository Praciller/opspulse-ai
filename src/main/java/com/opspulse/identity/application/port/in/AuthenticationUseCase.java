package com.opspulse.identity.application.port.in;

import com.opspulse.identity.application.AuthenticationResult;

public interface AuthenticationUseCase {

    AuthenticationResult login(LoginCommand command);

    AuthenticationResult refresh(String refreshToken);

    void logout(String refreshToken);

    record LoginCommand(String email, String password, String requestId, String ipAddress) {}
}
