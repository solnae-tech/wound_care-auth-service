package wound.detection.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "contact_value", length = 150)
    private String contactValue;

    /** Integer in the DB diagram (6-digit code stored as integer). */
    @Column(name = "otp_code", nullable = false)
    private Integer otpCode;

    @Column(name = "expiry_time")
    private LocalDateTime expiryTime;

    @Builder.Default
    @Column(name = "is_used")
    private Boolean isUsed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
