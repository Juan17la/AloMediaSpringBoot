package com.peciatech.alomediabackend.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from.email}")
    private String fromEmail;

    @Value("${app.recovery-base-url}")
    private String recoveryBaseUrl;

    public void sendEmail(String to, String subject, String body) {
        sendViaResend(to, subject, body);
    }

    public void sendPasswordRecoveryEmail(String toEmail, String token) {
        String link = recoveryBaseUrl + "?token=" + token;
        String text = "Click the link to reset your password: " + link;

        sendViaResend(toEmail, "Password Recovery Request", text);
    }

    private void sendViaResend(String to, String subject, String text) {
        String url = "https://api.resend.com/emails";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, String> body = new HashMap<>();
        body.put("from", fromEmail);
        body.put("to", to);
        body.put("subject", subject);
        body.put("text", text);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to send email via Resend: " + response.getBody());
        }
    }
}
