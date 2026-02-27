package org.example.projet_pi.Controller;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.PaymentDTO;
import org.example.projet_pi.Service.IPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final IPaymentService paymentService;

    @PostMapping("/addPayment")
    public PaymentDTO addPayment(
            @RequestBody PaymentDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {
        return paymentService.addPayment(dto, currentUser.getUsername());
    }

    @GetMapping("/getPayment/{id}")
    public PaymentDTO getPaymentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return paymentService.getPaymentById(id, currentUser.getUsername());
    }

    @GetMapping("/allPayments")
    public List<PaymentDTO> getAllPayments(
            @AuthenticationPrincipal UserDetails currentUser) {
        return paymentService.getAllPayments(currentUser.getUsername());
    }

    @GetMapping("/contract/{contractId}")
    public List<PaymentDTO> getPaymentsByContract(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails currentUser) {
        return paymentService.getPaymentsByContractId(contractId, currentUser.getUsername());
    }

    // Endpoint Stripe pour créer un PaymentIntent
    @PostMapping("/create-payment-intent/{contractId}")
    public ResponseEntity<Map<String, String>> createPaymentIntent(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails currentUser) throws StripeException {

        // Vérifier d'abord les droits via le service
        paymentService.getPaymentsByContractId(contractId, currentUser.getUsername());

        PaymentIntent paymentIntent = paymentService.createStripePaymentIntent(contractId);

        Map<String, String> response = new HashMap<>();
        response.put("clientSecret", paymentIntent.getClientSecret());

        return ResponseEntity.ok(response);
    }

    // Webhook Stripe (pas de sécurité car appelé par Stripe)
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload) {
        // Traiter le webhook Stripe
        // Récupérer le PaymentIntent et appeler handleSuccessfulPayment
        return ResponseEntity.ok("Webhook reçu");
    }
}