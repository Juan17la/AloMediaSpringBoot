package com.peciatech.alomediabackend.auth.service;

import com.peciatech.alomediabackend.user.entity.User;
import com.peciatech.alomediabackend.user.enums.Role;
import com.peciatech.alomediabackend.user.repository.UserRepository;
import com.peciatech.alomediabackend.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String handleOAuth2Login(String email, String name, String provider) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createOAuth2User(email, name, provider));

        return jwtService.generateToken(user);
    }

    private User createOAuth2User(String email, String name, String provider) {
        String[] parts = name != null ? name.split(" ", 2) : new String[]{""};
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.USER)
                .enabled(true)
                .provider(provider)
                .build();

        return userRepository.save(user);
    }
}
