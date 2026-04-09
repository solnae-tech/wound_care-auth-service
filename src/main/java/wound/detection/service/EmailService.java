package wound.detection.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String senderEmail;

    public void sendOtpEmail(String to, int otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(to);
        message.setSubject("Your OTP Verification Code");
        message.setText(
                "Your OTP code is: " + otpCode + "\n\n" +
                        "This code will expire in 5 minutes.\n" +
                        "If you did not request this, please ignore this email."
        );
        try {
            System.out.println("Attempting to send OTP email to: " + to);
            mailSender.send(message);
            System.out.println("OTP email successfully sent to: " + to);
        } catch (Exception e) {
            System.err.println("Failed to send OTP email to " + to + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send email. Please try again later.");
        }
    }
}