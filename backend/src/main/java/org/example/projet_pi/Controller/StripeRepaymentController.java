//package org.example.projet_pi.Controller;
//
//import com.stripe.exception.SignatureVerificationException;
//import com.stripe.model.Event;
//import com.stripe.model.PaymentIntent;
//import com.stripe.net.Webhook;
//import org.example.projet_pi.Service.RepaymentService;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/stripe")
//public class StripeRepaymentController {
//
//    private final RepaymentService repaymentService;
//
//    @Value("${stripe.webhook.secret}")
//    private String webhookSecret;
//
//    public StripeWebhookController(RepaymentService repaymentService) {
//        this.repaymentService = repaymentService;
//    }
//
//    @PostMapping("/webhook")
//    public ResponseEntity<String> handleWebhook(
//            @RequestBody String payload,
//            @RequestHeader("Stripe-Signature") String sigHeader) {
//
//        try {
//            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
//
//            switch (event.getType()) {
//                case "payment_intent.succeeded":
//                    PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
//                    handleSuccessfulPayment(paymentIntent);
//                    break;
//                case "payment_intent.payment_failed":
//                    handleFailedPayment(event);
//                    break;
//                default:
//                    System.out.println("Unhandled event type: " + event.getType());
//            }
//
//            return ResponseEntity.ok().build();
//
//        } catch (SignatureVerificationException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error");
//        }
//    }
//
//    private void handleSuccessfulPayment(PaymentIntent paymentIntent) {
//        Long creditId = Long.parseLong(paymentIntent.getMetadata().get("creditId"));
//        Long amount = paymentIntent.getAmount(); // En centimes
//
//        // Convertir en dinars (diviser par 100)
//        java.math.BigDecimal amountInDinars = java.math.BigDecimal.valueOf(amount)
//                .divide(java.math.BigDecimal.valueOf(100));
//
//        repaymentService.processSuccessfulStripePayment(creditId, amountInDinars);
//    }
//
//    private void handleFailedPayment(Event event) {
//        // Logique pour les paiements échoués
//        System.out.println("Paiement échoué: " + event.getId());
//    }
//}