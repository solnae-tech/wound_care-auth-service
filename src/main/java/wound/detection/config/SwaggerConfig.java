package wound.detection.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger / OpenAPI 3 configuration.
 *
 * <p>Swagger UI: <a href="http://localhost:8080/swagger-ui/index.html">
 *     http://localhost:8080/swagger-ui/index.html</a></p>
 * <p>OpenAPI JSON: <a href="http://localhost:8080/v3/api-docs">
 *     http://localhost:8080/v3/api-docs</a></p>
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // ── API metadata ──────────────────────────────────────────────
                .info(new Info()
                        .title("Wound Detection — Auth API")
                        .description("""
                                ## Authentication Flow
                                
                                ### Email / Password + OTP
                                1. `POST /api/auth/register` — create account (OTP sent to email)
                                2. `POST /api/auth/login`    — sign in (OTP sent to email)
                                3. `POST /api/auth/verify-otp` — submit OTP → receive **JWT**
                                4. Use the JWT as `Authorization: Bearer <token>` on all secured endpoints
                                5. `POST /api/auth/logout`  — invalidate the session
                                
                                ### Google OAuth2
                                Open **GET /api/auth/google** in a browser.  
                                Google will redirect back and the response will contain the **JWT**.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Vengadeshwaran K")
                                .email("vengadeshwaran558@gmail.com"))
                        .license(new License()
                                .name("Private")))

                // ── Server list ───────────────────────────────────────────────
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")))

                // ── JWT bearer scheme (shown in "Authorize" button) ───────────
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                            "Paste the JWT token received from /api/auth/verify-otp " +
                                            "or the Google OAuth2 callback.")));
    }
}
