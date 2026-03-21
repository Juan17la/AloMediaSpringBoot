package com.peciatech.alomediabackend.auth.service;

import com.peciatech.alomediabackend.auth.dto.request.LoginRequest;
import com.peciatech.alomediabackend.auth.dto.request.RegisterRequest;
import com.peciatech.alomediabackend.auth.dto.request.ResetPasswordRequest;
import com.peciatech.alomediabackend.user.entity.RecoveryToken;
import com.peciatech.alomediabackend.user.entity.User;
import com.peciatech.alomediabackend.user.enums.Role;
import com.peciatech.alomediabackend.user.repository.RecoveryTokenRepository;
import com.peciatech.alomediabackend.user.repository.UserRepository;
import com.peciatech.alomediabackend.common.exception.*;
import com.peciatech.alomediabackend.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RecoveryTokenRepository recoveryTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${app.recovery-base-url}")
    private String recoveryBaseUrl;

    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .provider("local")
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        return jwtService.generateToken(saved);
    }

    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return jwtService.generateToken(user);
    }


    public void requestPasswordRecovery(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("No account found with that email"));

        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);

        RecoveryToken recoveryToken = RecoveryToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        recoveryTokenRepository.save(recoveryToken);
        emailService.sendPasswordRecoveryEmail(email, token);
    }


    public boolean validateRecoveryToken(String token) {
        RecoveryToken recoveryToken = recoveryTokenRepository.findByToken(token)
                .orElseThrow(() -> new RecoveryTokenNotFoundException("Recovery token not found"));

        if (LocalDateTime.now().isAfter(recoveryToken.getExpiresAt())) {
            throw new RecoveryTokenExpiredException("Recovery token has expired");
        }

        if (recoveryToken.isUsed()) {
            throw new RecoveryTokenAlreadyUsedException("Recovery token has already been used");
        }

        return true;
    }


    public void resetPassword(ResetPasswordRequest request) {
        validateRecoveryToken(request.getToken());

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords do not match");
        }

        RecoveryToken recoveryToken = recoveryTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RecoveryTokenNotFoundException("Recovery token not found"));

        User user = recoveryToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        recoveryToken.setUsed(true);
        recoveryTokenRepository.save(recoveryToken);
    }

    public User getCurrentUser(String jwtToken) {
        if (jwtToken == null || jwtToken.isBlank()) {
            return null;
        }
        if (!jwtService.validateToken(jwtToken)) {
            return null;
        }
        String email = jwtService.extractUsername(jwtToken);
        return userRepository.findByEmail(email).orElse(null);
    }
}
