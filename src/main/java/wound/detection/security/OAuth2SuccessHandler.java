package wound.detection.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import wound.detection.model.AuthSession;
import wound.detection.model.User;
import wound.detection.repository.AuthSessionRepository;
import wound.detection.repository.UserRepository;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * Called by Spring Security after a successful Google OAuth2 login.
 *
 * <p>Steps:
 * <ol>
 *   <li>Read the plain user fields (id, email, fullName) from the custom principal.
 *       These are primitives — never lazy proxies — so there is no
 *       {@code LazyInitializationException} risk.</li>
 *   <li>Reload the {@link User} entity from the DB (fresh transaction)
 *       so we have a managed instance to attach to the {@link AuthSession}.</li>
 *   <li>Generate a JWT.</li>
 *   <li>Persist an {@link AuthSession} row (same as the email/OTP login flow).</li>
 *   <li>Write a JSON response directly to the browser.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider      tokenProvider;
    private final AuthSessionRepository authSessionRepository;
    private final UserRepository        userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest  request,
                                        HttpServletResponse response,
                                        Authentication      authentication)
            throws IOException, ServletException {

        // 1 — Read plain values from principal (safe — no lazy proxy involved)
        OAuth2AuthenticatedPrincipal principal =
                (OAuth2AuthenticatedPrincipal) authentication.getPrincipal();

        Long   userId   = principal.getUserId();
        String email    = principal.getEmail();
        String fullName = principal.getFullName() == null ? "" : principal.getFullName();

        // 2 — Reload User as a fresh managed entity for the AuthSession FK
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 3 — Generate JWT
        String token = tokenProvider.generateToken(email);

        // 4 — Persist the session (same pattern as email/OTP verifyOtp)
        authSessionRepository.save(
                AuthSession.builder()
                        .user(user)
                        .accessToken(token)
                        .deviceInfo(request.getHeader("User-Agent"))
                        .ipAddress(request.getRemoteAddr())
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .build()
        );

        // 5 — Write JSON response directly (no frontend needed)
        //     Escape any special characters in fullName to keep the JSON valid.
        String safeName = fullName
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        String json = "{"
                + "\"token\":\""     + token    + "\","
                + "\"email\":\""     + email    + "\","
                + "\"fullName\":\""  + safeName + "\","
                + "\"requiresOtp\":" + false    + ","
                + "\"message\":\""   + "Google login successful." + "\""
                + "}";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }
}
