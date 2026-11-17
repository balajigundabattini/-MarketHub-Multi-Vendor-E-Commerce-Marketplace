package com.markethub.payments.application;
import java.math.BigDecimal;
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
    public RazorpayGateway(@Value("${markethub.payments.razorpay.key-id:}") String keyId,@Value("${markethub.payments.razorpay.key-secret:}") String keySecret,@Value("${markethub.payments.razorpay.webhook-secret:}") String webhookSecret,@Value("${markethub.payments.razorpay.base-url:https://api.razorpay.com/v1}") String baseUrl) { this.keyId=keyId; this.keySecret=keySecret; this.webhookSecret=webhookSecret; this.baseUrl=baseUrl; }
    public String createOrder(Long paymentId,BigDecimal amount,String currency) {
        if(keyId.isBlank()||keySecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Online payments are not configured");
        long minor=PaymentAmounts.minorUnits(amount,currency);
        try { Map<?,?> result=RestClient.create(baseUrl).post().uri("/orders").headers(h->h.setBasicAuth(keyId,keySecret)).body(Map.of("amount",minor,"currency",currency,"receipt","mh_"+paymentId+"_"+UUID.randomUUID().toString().substring(0,8))).retrieve().body(Map.class);
            Object id=result==null?null:result.get("id"); if(!(id instanceof String value)||value.isBlank()) throw new IllegalStateException("Razorpay did not return an order id"); return value;
        } catch(ResponseStatusException e){throw e;} catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Payment provider could not create an order",e);}
    }
    public String keyId(){return keyId;} public String keySecret(){return keySecret;} public String webhookSecret(){return webhookSecret;}
}
