package wound.detection.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import wound.detection.security.JwtAuthenticationFilter;
import wound.detection.security.OAuth2SuccessHandler;
import wound.detection.service.CustomOAuth2UserService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService  customOAuth2UserService;
    private final OAuth2SuccessHandler     oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── CSRF ──────────────────────────────────────────────────────────
            .csrf(csrf -> csrf.disable())

            // ── Session policy ────────────────────────────────────────────────
            // We keep the API layer stateless (JWT), but the OAuth2 authorization
            // code flow *requires* a short-lived server-side session to store the
            // state / nonce between the /oauth2/authorization redirect and the
            // /login/oauth2/code callback. IF_REQUIRED creates a session only
            // for that purpose; every other request is still JWT-secured.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            // ── 401 JSON for unauthenticated API calls ─────────────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}"
                    );
                })
            )

            // ── Public endpoints ───────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    // Auth endpoints
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/verify-otp",
                    "/api/auth/google",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    // Swagger UI & OpenAPI docs
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/webjars/**"
                ).permitAll()
                .anyRequest().authenticated()
            )

            // ── Google OAuth2 login flow ───────────────────────────────────────
            .oauth2Login(oauth2 -> oauth2
                // Store the state/nonce in the HTTP session for the OAuth2 dance
                .authorizationEndpoint(ae -> ae
                    .authorizationRequestRepository(new HttpSessionOAuth2AuthorizationRequestRepository()))
                .userInfoEndpoint(userInfo ->
                    userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
            )

            // ── JWT filter ────────────────────────────────────────────────────
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
