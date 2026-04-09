package wound.detection.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wound.detection.dto.AuthResponse;
import wound.detection.dto.LoginRequest;
import wound.detection.dto.RegistrationRequest;
import wound.detection.dto.VerifyOtpRequest;
import wound.detection.model.AuthSession;
import wound.detection.model.OtpVerification;
import wound.detection.model.User;
import wound.detection.model.UserProfile;
import wound.detection.repository.AuthSessionRepository;
import wound.detection.repository.OtpVerificationRepository;
import wound.detection.repository.UserProfileRepository;
import wound.detection.repository.UserRepository;
import wound.detection.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthSessionRepository authSessionRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final HttpServletRequest httpRequest;

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegistrationRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already in use.");
        }
        if (userRepository.existsByPhoneNumber(req.getPhoneNumber())) {
            throw new RuntimeException("Phone number already in use.");
        }

        User user = userRepository.save(
                User.builder()
                        .fullName(req.getFullName())
                        .email(req.getEmail())
                        .phoneNumber(req.getPhoneNumber())
                        .passwordHash(passwordEncoder.encode(req.getPassword()))
                        .isVerified(false)
                        .isActive(true)
                        .build()
        );

        // Create empty profile so the profile endpoint always succeeds after registration
        userProfileRepository.save(
                UserProfile.builder()
                        .user(user)
                        .fullName(user.getFullName())
                        .phoneNumber(user.getPhoneNumber())
                        .build()
        );

        sendOtp(user);

        return AuthResponse.builder()
                .email(user.getEmail())
                .requiresOtp(true)
                .message("Registration successful. Check your email for the OTP to verify your account.")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest req) {
        // Validate credentials — throws BadCredentialsException if wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (!user.getIsActive()) {
            throw new RuntimeException("Account is deactivated.");
        }

        sendOtp(user);

        return AuthResponse.builder()
                .email(user.getEmail())
                .requiresOtp(true)
                .message("OTP sent to your registered email. Please verify to continue.")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY OTP  →  issues JWT
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found."));

        OtpVerification otp = otpVerificationRepository
                .findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("No active OTP found. Please request a new one."));

        // Compare — DB stores Integer, client sends 6-digit String
        int clientOtp = Integer.parseInt(req.getOtpCode());
        if (!otp.getOtpCode().equals(clientOtp)) {
            throw new RuntimeException("Incorrect OTP. Please try again.");
        }

        if (LocalDateTime.now().isAfter(otp.getExpiryTime())) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        // Mark OTP consumed
        otp.setIsUsed(true);
        otpVerificationRepository.save(otp);

        // Mark user verified (first-time login after registration)
        if (!user.getIsVerified()) {
            user.setIsVerified(true);
            userRepository.save(user);
        }

        // Generate JWT
        String token = tokenProvider.generateToken(user.getEmail(), user.getId(), user.getFullName());

        // Persist session record
        authSessionRepository.save(
                AuthSession.builder()
                        .user(user)
                        .accessToken(token)
                        .deviceInfo(httpRequest.getHeader("User-Agent"))
                        .ipAddress(httpRequest.getRemoteAddr())
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .build()
        );

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .requiresOtp(false)
                .message("Login successful.")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
        authSessionRepository.deleteByUser(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void sendOtp(User user) {
        // Generate a random 6-digit integer (100000 – 999999)
        int otpCode = 100000 + new Random().nextInt(900000);

        otpVerificationRepository.save(
                OtpVerification.builder()
                        .user(user)
                        .contactValue(user.getEmail())
                        .otpCode(otpCode)
                        .expiryTime(LocalDateTime.now().plusMinutes(5))
                        .isUsed(false)
                        .build()
        );

        emailService.sendOtpEmail(user.getEmail(), otpCode);
    }
}