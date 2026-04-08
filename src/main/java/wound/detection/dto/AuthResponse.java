package wound.detection.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String fullName;
    private boolean requiresOtp;
    private String message;
}
