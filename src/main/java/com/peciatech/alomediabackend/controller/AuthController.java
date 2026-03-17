package com.peciatech.alomediabackend.controller;

import com.peciatech.alomediabackend.dto.request.LoginRequest;
import com.peciatech.alomediabackend.dto.request.RecoverRequestDTO;
import com.peciatech.alomediabackend.dto.request.RegisterRequest;
import com.peciatech.alomediabackend.dto.request.ResetPasswordRequest;
import com.peciatech.alomediabackend.dto.response.AuthResponse;
import com.peciatech.alomediabackend.dto.response.CurrentUserResponse;
import com.peciatech.alomediabackend.dto.response.RecoveryValidationResponse;
import com.peciatech.alomediabackend.entity.User;
import com.peciatech.alomediabackend.mapper.UserMapper;
import com.peciatech.alomediabackend.security.cookie.CookieService;
import com.peciatech.alomediabackend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final CookieService cookieService;

    // Registration & Login 

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        String token = authService.register(request);
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieService.createAuthCookie(token).toString());

        User user = authService.getCurrentUser(token);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.toAuthResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        String token = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieService.createAuthCookie(token).toString());

        User user = authService.getCurrentUser(token);
        return ResponseEntity.ok(userMapper.toAuthResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieService.createClearCookie().toString());
        return ResponseEntity.ok().build();
    }

    // Password Recovery

    @PostMapping("/recover/request")
    public ResponseEntity<Void> requestRecovery(@Valid @RequestBody RecoverRequestDTO request) {
        try {
            authService.requestPasswordRecovery(request.getEmail());
        } catch (Exception ignored) {
            // Do not reveal whether the email exists
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/recover/validate")
    public ResponseEntity<RecoveryValidationResponse> validateToken(@RequestParam String token) {
        boolean valid;
        try {
            valid = authService.validateRecoveryToken(token);
        } catch (Exception e) {
            valid = false;
        }
        return ResponseEntity.ok(RecoveryValidationResponse.builder().valid(valid).build());
    }

    @PostMapping("/recover/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    // Current User

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(HttpServletRequest request) {
        String token = null;
        if (request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(c -> "access_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        User user = authService.getCurrentUser(token);
        if (user != null) {
            return ResponseEntity.ok(CurrentUserResponse.builder()
                    .authenticated(true)
                    .user(userMapper.toUserResponse(user))
                    .build());
        }
        return ResponseEntity.ok(CurrentUserResponse.builder()
                .authenticated(false)
                .user(null)
                .build());
    }
}
