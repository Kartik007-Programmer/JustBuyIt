package com.example.JustBuyIt.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String to, String token) {
        String link = baseUrl + "/auth/v1/verify-email?token=" + token;
        send(to, "Verify your JustBuyIt account",
                "Welcome to JustBuyIt!\n\nPlease verify your email by clicking the link below:\n"
                        + link + "\n\nThis link expires in 24 hours.");
    }

    public void sendPasswordResetEmail(String to, String token) {
        String link = baseUrl + "/ResetPassword.html?token=" + token;
        send(to, "Reset your JustBuyIt password",
                "We received a request to reset your password.\n\nClick the link below:\n"
                        + link + "\n\nThis link expires in 30 minutes. If you didn't request this, ignore this email.");
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}