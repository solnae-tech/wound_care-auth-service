package wound.detection.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wound.detection.dto.AuthResponse;
import wound.detection.dto.LoginRequest;
import wound.detection.dto.RegistrationRequest;
import wound.detection.dto.VerifyOtpRequest;
import wound.detection.service.AuthService;

import java.io.IOException;
import java.security.Principal;

@Tag(name = "Authentication", description = "Register, Login (OTP), Verify OTP, Logout, and Google OAuth2")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── Google OAuth2 ────────────────────────────────────────────────────────

    @Operation(
        summary     = "Google OAuth2 Login / Register",
        description = "Opens this URL in a **browser** — it redirects to Google's consent screen. "
                    + "After the user selects their account, Google calls back to the backend. "
                    + "The response body will contain the JWT token directly (JSON). "
                    + "Data is saved to `users`, `oauth_accounts`, `user_profiles`, and `auth_sessions`."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to Google consent screen"),
        @ApiResponse(responseCode = "200", description = "JWT returned after successful Google login",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    })
    @GetMapping("/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    // ── Register ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Register a new user",
        description = "Creates the user account and sends a **6-digit OTP** to the provided email. "
                    + "Call `/api/auth/verify-otp` next to get the JWT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent — requiresOtp=true",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error or email/phone already in use",
            content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Login with email + password",
        description = "Validates credentials and sends a **6-digit OTP** to the user's email. "
                    + "Call `/api/auth/verify-otp` next to get the JWT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent — requiresOtp=true",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Bad credentials",
            content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── Verify OTP ────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Verify OTP and receive JWT",
        description = "Submits the 6-digit OTP received by email. On success, returns a **JWT token** "
                    + "that must be sent as `Authorization: Bearer <token>` on all secured requests."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "JWT issued — requiresOtp=false",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Incorrect or expired OTP",
            content = @Content)
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Logout",
        description = "Invalidates **all active sessions** for the authenticated user. "
                    + "Requires `Authorization: Bearer <token>` header."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logged out successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content)
    })
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(Principal principal) {
        authService.logout(principal.getName());
        return ResponseEntity.ok(
            AuthResponse.builder()
                .message("Logged out successfully.")
                .build()
        );
    }
}
