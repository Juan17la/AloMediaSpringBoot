package com.peciatech.alomediabackend.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.recovery-base-url}")
    private String recoveryBaseUrl;

    // I did create an email for sending all the recovery emails...
    // let's hope it works and doesn't end up in spam :D
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void sendPasswordRecoveryEmail(String toEmail, String token) {
        String link = recoveryBaseUrl + "?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Recovery Request");
        message.setText("Click the link to reset your password: " + link);

        mailSender.send(message);
    }
}
