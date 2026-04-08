package wound.detection.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String to, int otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your OTP Verification Code");
        message.setText(
            "Your OTP code is: " + otpCode + "\n\n" +
            "This code will expire in 5 minutes.\n" +
            "If you did not request this, please ignore this email."
        );
        mailSender.send(message);
    }
}
