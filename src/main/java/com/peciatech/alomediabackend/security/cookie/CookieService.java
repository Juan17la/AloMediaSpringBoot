package com.peciatech.alomediabackend.security.cookie;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

    @Value("${cookie.secure}")
    private boolean secure;

    @Value("${jwt.expiration}")
    private long expiration;

    public ResponseCookie createAuthCookie(String token) {
        return ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("None")
                .path("/")
                .maxAge(expiration / 1000)
                .build();
    }

    public ResponseCookie createClearCookie() {
        return ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
    }
}
