package com.opspulse.identity.api;

import com.opspulse.identity.api.dto.AuthenticationResponse;
import com.opspulse.identity.api.dto.LoginRequest;
import com.opspulse.identity.api.dto.RefreshTokenRequest;
import com.opspulse.identity.api.dto.RegisterRequest;
import com.opspulse.identity.api.dto.UserResponse;
import com.opspulse.identity.application.port.in.AuthenticationUseCase;
import com.opspulse.identity.application.port.in.RegisterUserUseCase;
import com.opspulse.identity.domain.Role;
import com.opspulse.shared.config.OpenApiConfig;
import com.opspulse.shared.error.ApiErrorResponse;
import com.opspulse.shared.observability.RequestIdContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthenticationUseCase authentication;
    private final RegisterUserUseCase registration;

    public AuthController(
            AuthenticationUseCase authentication, RegisterUserUseCase registration) {
        this.authentication = authentication;
        this.registration = registration;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated"),
        @ApiResponse(
                responseCode = "401",
                description = "Invalid credentials",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public AuthenticationResponse login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        var command = new AuthenticationUseCase.LoginCommand(
                request.email(),
                request.password(),
                RequestIdContext.currentRequestId(),
                servletRequest.getRemoteAddr());
        return AuthenticationResponse.from(authentication.login(command));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token rotated"),
        @ApiResponse(
                responseCode = "401",
                description = "Invalid, expired, or replayed refresh token",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public AuthenticationResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return AuthenticationResponse.from(authentication.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token")
    @ApiResponse(responseCode = "204", description = "Token revoked or already inactive")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authentication.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a user",
            description =
                    "ADMIN_ONLY requires an ADMIN bearer token. PUBLIC_VIEWER permits public "
                            + "registration but only for the VIEWER role.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Role not permitted",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Email already exists",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            Authentication currentAuthentication,
            HttpServletRequest servletRequest) {
        UUID actorUserId = actorUserId(currentAuthentication);
        Set<Role> actorRoles = actorRoles(currentAuthentication);
        var user = registration.register(new RegisterUserUseCase.RegisterUserCommand(
                request.email(),
                request.password(),
                request.fullName(),
                request.roles(),
                actorUserId,
                actorRoles,
                RequestIdContext.currentRequestId(),
                servletRequest.getRemoteAddr()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    private static UUID actorUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt && jwt.isAuthenticated()) {
            return UUID.fromString(jwt.getToken().getSubject());
        }
        return null;
    }

    private static Set<Role> actorRoles(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> Role.valueOf(authority.substring("ROLE_".length())))
                .collect(Collectors.toUnmodifiableSet());
    }
}
