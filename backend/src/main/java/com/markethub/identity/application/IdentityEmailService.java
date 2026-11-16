package com.markethub.identity.application;

import java.time.Duration;

import com.markethub.identity.infrastructure.jpa.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class IdentityEmailService {
    private final JavaMailSender mailSender;
    private final IdentityActionTokenService tokenService;
    private final String frontendOrigin;
    private final String fromAddress;

    public IdentityEmailService(JavaMailSender mailSender, IdentityActionTokenService tokenService,
            @Value("${markethub.security.frontend-origin}") String frontendOrigin,
            @Value("${markethub.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.tokenService = tokenService;
        this.frontendOrigin = frontendOrigin;
        this.fromAddress = fromAddress;
    }

    public void sendVerification(UserEntity user) {
        String token = tokenService.issue(user, "EMAIL_VERIFICATION", Duration.ofHours(24));
        send(user.getEmail(), "Verify your MarketHub email",
                "Verify your email within 24 hours: " + frontendOrigin + "/verify-email?token=" + token);
    }

    public void sendPasswordReset(UserEntity user) {
        String token = tokenService.issue(user, "PASSWORD_RESET", Duration.ofMinutes(30));
        send(user.getEmail(), "Reset your MarketHub password",
                "Choose a new password within 30 minutes: " + frontendOrigin + "/reset-password?token=" + token);
    }

    private void send(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
