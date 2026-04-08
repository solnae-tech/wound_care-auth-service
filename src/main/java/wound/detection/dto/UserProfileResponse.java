package wound.detection.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read-only response DTO for user profile data. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String location;
    private String bloodType;

    /** true = has high blood pressure */
    private Boolean bloodPressure;

    /** true = has elevated blood sugar / diabetes */
    private Boolean bloodSugar;

    private BigDecimal weight;
    private BigDecimal height;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
