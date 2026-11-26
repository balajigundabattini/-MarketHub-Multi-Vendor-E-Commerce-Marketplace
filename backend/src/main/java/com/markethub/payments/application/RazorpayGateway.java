
package com.markethub.payments.application;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RazorpayGateway {

    private final String keyId, keySecret, webhookSecret, baseUrl;

    public RazorpayGateway(
            @Value("${markethub.payments.razorpay.key-id:}") String keyId,
            @Value("${markethub.payments.razorpay.key-secret:}") String keySecret,
            @Value("${markethub.payments.razorpay.webhook-secret:}") String webhookSecret,
            @Value("${markethub.payments.razorpay.base-url:https://api.razorpay.com/v1}") String baseUrl) {

        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.baseUrl = baseUrl;
    }

    public String createOrder(Long paymentId, BigDecimal amount, String currency) {

        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Online payments are not configured");
        }

        long minor = PaymentAmounts.minorUnits(amount, currency);

        try {
            Map<?, ?> result = RestClient.create(baseUrl)
                    .post()
                    .uri("/orders")
                    .headers(h -> h.setBasicAuth(keyId, keySecret))
                    .body(Map.of(
                            "amount", minor,
                            "currency", currency,
                            "receipt", "mh_" + paymentId + "_"
                                    + UUID.randomUUID().toString().substring(0, 8)))
                    .retrieve()
                    .body(Map.class);

            Object id = result == null ? null : result.get("id");

            if (!(id instanceof String value) || value.isBlank()) {
                throw new IllegalStateException("Razorpay did not return an order id");
            }

            return value;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Payment provider could not create an order", e);
        }
    }

    // Existing full refund method. Keeps the current functionality unchanged.
    public RefundResult refund(String paymentId, String reason) {
        return createRefund(paymentId, null, null, reason);
    }

    // New partial refund method.
    public RefundResult refund(
            String paymentId,
            BigDecimal amount,
            String currency,
            String reason) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }

        long minor = PaymentAmounts.minorUnits(amount, currency);

        return createRefund(paymentId, minor, currency, reason);
    }

    private RefundResult createRefund(
            String paymentId,
            Long amount,
            String currency,
            String reason) {

        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Online refunds are not configured");
        }

        try {
            Map<String, Object> request = new LinkedHashMap<>();

            // Razorpay requires amount in minor units for partial refunds.
            // If omitted, the existing full-refund behavior is preserved.
            if (amount != null) {
                request.put("amount", amount);
            }

            request.put("notes", Map.of("reason", reason));

            Map<?, ?> result = RestClient.create(baseUrl)
                    .post()
                    .uri("/payments/{id}/refund", paymentId)
                    .headers(h -> h.setBasicAuth(keyId, keySecret))
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (result == null || !(result.get("id") instanceof String id)) {
                throw new IllegalStateException("Razorpay did not return a refund id");
            }

            Object status = result.containsKey("status")
                    ? result.get("status")
                    : "pending";

            return new RefundResult(id, String.valueOf(status));

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Payment provider could not create a refund", e);
        }
    }

    public record RefundResult(String id, String status) {
    }

    public String keyId() {
        return keyId;
    }

    public String keySecret() {
        return keySecret;
    }

    public String webhookSecret() {
        return webhookSecret;
    }
}
