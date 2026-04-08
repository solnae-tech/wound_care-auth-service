package wound.detection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wound.detection.model.OAuthAccount;
import wound.detection.model.User;

import java.util.Optional;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    Optional<OAuthAccount> findByUserAndProvider(User user, String provider);
}
