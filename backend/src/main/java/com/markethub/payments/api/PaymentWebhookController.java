package com.markethub.payments.api;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.markethub.payments.application.PaymentService;
import com.markethub.payments.application.RazorpayGateway;
import com.markethub.orders.application.AdminOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class PaymentWebhookController {
    private final PaymentService payments; private final AdminOrderService adminOrders; private final RazorpayGateway gateway; private final ObjectMapper mapper;
    public PaymentWebhookController(PaymentService payments,AdminOrderService adminOrders,RazorpayGateway gateway,ObjectMapper mapper){this.payments=payments;this.adminOrders=adminOrders;this.gateway=gateway;this.mapper=mapper;}
    @PostMapping(value="/api/v1/payments/webhook",consumes="application/json") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void webhook(@RequestHeader(value="X-Razorpay-Signature",required=false) String signature,@RequestBody byte[] raw) {
        if(!valid(raw,signature)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Webhook signature is invalid");
        try { JsonNode root=mapper.readTree(raw); String event=root.path("event").asText();
            if(event.equals("refund.processed")||event.equals("refund.failed")) {
                String refundId=root.path("payload").path("refund").path("entity").path("id").asText(null);
                if(refundId!=null)adminOrders.refundWebhook(refundId,event);
                return;
            }
            if(!event.equals("payment.captured")&&!event.equals("payment.failed"))return;
            JsonNode entity=root.path("payload").path("payment").path("entity"); if(entity.isMissingNode())entity=root.path("payload").path("order").path("entity");
            String orderId=entity.path("order_id").asText(null), paymentId=entity.path("id").asText(null);
            if(orderId==null||paymentId==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Webhook payment details are missing");
            payments.webhook(orderId,paymentId,event,entity.path("amount").asLong(-1),entity.path("currency").asText(""));
        } catch(ResponseStatusException e){throw e;} catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Webhook payload is invalid",e);}
    }
    private boolean valid(byte[] body,String signature){if(signature==null||gateway.webhookSecret().isBlank())return false;try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(gateway.webhookSecret().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));String expected=java.util.HexFormat.of().formatHex(mac.doFinal(body));return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),signature.getBytes(StandardCharsets.US_ASCII));}catch(Exception e){return false;}}
}
