package wound.detection.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String location;

    @Column(name = "blood_type", length = 10)
    private String bloodType;

    /** Boolean in the diagram — true means high blood-pressure flag */
    @Column(name = "blood_pressure")
    private Boolean bloodPressure;

    /** Boolean in the diagram — true means elevated blood-sugar / diabetic flag */
    @Column(name = "blood_sugar")
    private Boolean bloodSugar;

    /** Numeric(precision=6, scale=2) — weight in kg */
    @Column(precision = 6, scale = 2)
    private BigDecimal weight;

    /** Numeric(precision = 5, scale = 2) — height in cm */
    @Column(precision = 5, scale = 2)
    private BigDecimal height;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
