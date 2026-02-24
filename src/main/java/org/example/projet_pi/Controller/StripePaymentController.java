package org.example.projet_pi.Controller;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments/stripe")
@AllArgsConstructor
public class StripePaymentController {

    private final PaymentService paymentService;

    // Créer un PaymentIntent pour frontend
    @PostMapping("/create-payment-intent/{contractId}")
    public Map<String, String> createPaymentIntent(@PathVariable Long contractId) throws StripeException {
        PaymentIntent intent = paymentService.createStripePaymentIntent(contractId);

        Map<String, String> response = new HashMap<>();
        response.put("clientSecret", intent.getClientSecret());

        return response;
    }

    // Webhook Stripe
    @PostMapping("/webhook")
    public String handleWebhook(@RequestBody String payload,
                                @RequestHeader("Stripe-Signature") String sigHeader) {

        String endpointSecret = "whsec_3a6d8e127e97f4e5e1534bae05fd570a220df95e3e0efc0cfd5046db239e7f55";

        try {
            com.stripe.model.Event event = com.stripe.net.Webhook.constructEvent(payload, sigHeader, endpointSecret);

            if ("payment_intent.succeeded".equals(event.getType())) {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
                paymentService.handleSuccessfulPayment(intent.getId(), intent.getAmount());
            }

            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Erreur";
        }
    }
}