package wound.detection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wound.detection.model.AuthSession;
import wound.detection.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    List<AuthSession> findByUser(User user);
    Optional<AuthSession> findByAccessToken(String accessToken);
    void deleteByUser(User user);
}
