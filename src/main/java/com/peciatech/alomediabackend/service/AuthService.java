package com.peciatech.alomediabackend.service;

import com.peciatech.alomediabackend.dto.request.LoginRequest;
import com.peciatech.alomediabackend.dto.request.RegisterRequest;
import com.peciatech.alomediabackend.dto.request.ResetPasswordRequest;
import com.peciatech.alomediabackend.entity.RecoveryToken;
import com.peciatech.alomediabackend.entity.User;
import com.peciatech.alomediabackend.enums.Role;
import com.peciatech.alomediabackend.exception.*;
import com.peciatech.alomediabackend.repository.RecoveryTokenRepository;
import com.peciatech.alomediabackend.repository.UserRepository;
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

    /**
     * Registers a new local user and returns a JWT token.
     */
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

    /**
     * Authenticates a local user and returns a JWT token.
     */
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return jwtService.generateToken(user);
    }

    /**
     * Initiates a password recovery flow: generates a secure token, persists it,
     * and sends a recovery email.
     */
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

    /**
     * Validates that a recovery token exists, is not expired, and has not been used.
     */
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

    /**
     * Resets the user's password using a validated recovery token.
     */
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

    /**
     * Returns the current user from a JWT cookie value. Returns null gracefully
     * if the token is missing, invalid, or the user no longer exists.
     */
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
