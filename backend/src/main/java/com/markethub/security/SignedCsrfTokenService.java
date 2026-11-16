package com.markethub.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class SignedCsrfTokenService {

    private final SecretKey signingKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public SignedCsrfTokenService(SecretKey signingKey) {
        this.signingKey = new SecretKeySpec(signingKey.getEncoded(), "HmacSHA256");
    }

    public String create() {
        byte[] nonce = new byte[32];
        secureRandom.nextBytes(nonce);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
        return value + "." + sign(value);
    }

    public boolean isValid(String token) {
        if (token == null) return false;
        int separator = token.indexOf('.');
        if (separator <= 0 || separator == token.length() - 1) return false;
        String nonce = token.substring(0, separator);
        String suppliedSignature = token.substring(separator + 1);
        byte[] expected = sign(nonce).getBytes(StandardCharsets.US_ASCII);
        byte[] supplied = suppliedSignature.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, supplied);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign CSRF token", exception);
        }
    }
}
