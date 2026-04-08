package wound.detection.dto;

import lombok.Data;
import java.math.BigDecimal;

/** Request body for updating profile information. */
@Data
public class UpdateProfileRequest {
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
}
