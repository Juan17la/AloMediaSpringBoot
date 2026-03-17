package com.peciatech.alomediabackend.security.config;

import com.peciatech.alomediabackend.exception.OAuth2AuthenticationException;
import com.peciatech.alomediabackend.security.cookie.CookieService;
import com.peciatech.alomediabackend.service.OAuth2Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2Service oAuth2Service;
    private final CookieService cookieService;

    @Value("${oauth2.frontend-redirect-url}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String provider = ((OAuth2AuthenticationToken) authentication)
                .getAuthorizedClientRegistrationId();

        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException(
                    "Email not available from " + provider + " OAuth2 provider");
        }

        String name = oAuth2User.getAttribute("name");
        if (name == null) {
            name = oAuth2User.getAttribute("login"); // GitHub fallback
        }
        if (name == null) {
            name = "";
        }

        String jwt = oAuth2Service.handleOAuth2Login(email, name, provider);

        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieService.createAuthCookie(jwt).toString());
        response.sendRedirect(frontendRedirectUrl);
    }
}
