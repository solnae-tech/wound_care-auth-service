package wound.detection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wound.detection.model.OtpVerification;
import wound.detection.model.User;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /** Finds the most recent unused OTP for a given user (ordered by created_at DESC). */
    Optional<OtpVerification> findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(User user);
}
