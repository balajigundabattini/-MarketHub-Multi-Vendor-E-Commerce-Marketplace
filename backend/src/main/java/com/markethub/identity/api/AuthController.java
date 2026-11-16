package com.markethub.identity.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.markethub.identity.application.AuthService;
import com.markethub.identity.application.AuthService.AuthSession;
import com.markethub.security.SignedCsrfTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "MARKETHUB_REFRESH";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String AUTH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final SignedCsrfTokenService csrfTokenService;
    private final boolean secureCookies;

    public AuthController(
            AuthService authService,
            SignedCsrfTokenService csrfTokenService,
            @Value("${markethub.security.refresh-cookie-secure}") boolean secureCookies) {
        this.authService = authService;
        this.csrfTokenService = csrfTokenService;
        this.secureCookies = secureCookies;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return sessionResponse(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return sessionResponse(authService.login(request), HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(value = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(value = "X-XSRF-TOKEN", required = false) String csrfHeader) {
        validateCsrf(csrfCookie, csrfHeader);
        return sessionResponse(authService.refresh(refreshToken), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(value = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(value = "X-XSRF-TOKEN", required = false) String csrfHeader) {
        if (refreshToken != null) validateCsrf(csrfCookie, csrfHeader);
        authService.logout(refreshToken);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, clearCookie(REFRESH_COOKIE, true).toString());
        headers.add(HttpHeaders.SET_COOKIE, clearCookie(CSRF_COOKIE, false).toString());
        return ResponseEntity.noContent().headers(headers).build();
    }

    private ResponseEntity<AuthResponse> sessionResponse(AuthSession session, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, session.refreshToken(), true,
                session.refreshLifetime()).toString());
        headers.add(HttpHeaders.SET_COOKIE, cookie(CSRF_COOKIE, session.csrfToken(), false,
                session.refreshLifetime()).toString());
        return ResponseEntity.status(status).headers(headers).body(session.response());
    }

    private void validateCsrf(String cookie, String header) {
        if (cookie == null || header == null
                || !MessageDigest.isEqual(cookie.getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8))
                || !csrfTokenService.isValid(cookie)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CSRF token is missing or invalid");
        }
    }

    private ResponseCookie cookie(String name, String value, boolean httpOnly, java.time.Duration lifetime) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(secureCookies)
                .path(name.equals(CSRF_COOKIE) ? "/" : AUTH_COOKIE_PATH)
                .sameSite("Strict")
                .maxAge(lifetime)
                .build();
    }

    private ResponseCookie clearCookie(String name, boolean httpOnly) {
        return ResponseCookie.from(name, "")
                .httpOnly(httpOnly)
                .secure(secureCookies)
                .path(name.equals(CSRF_COOKIE) ? "/" : AUTH_COOKIE_PATH)
                .sameSite("Strict")
                .maxAge(java.time.Duration.ZERO)
                .build();
    }
}
