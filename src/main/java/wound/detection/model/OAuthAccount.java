package wound.detection.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_accounts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider", "provider_user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** e.g. "google", "facebook" */
    @Column(nullable = false, length = 50)
    private String provider;

    /** The unique user ID as returned by the OAuth provider */
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
