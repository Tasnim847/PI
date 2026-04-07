package org.example.projet_pi.Controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Service.PaymentService;
import org.example.projet_pi.entity.InsuranceContract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payments/stripe")
public class StripePaymentController {

    private final PaymentService paymentService;
    private final InsuranceContractRepository contractRepository;
    private final Gson gson = new Gson();

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    public StripePaymentController(PaymentService paymentService, InsuranceContractRepository contractRepository) {
        this.paymentService = paymentService;
        this.contractRepository = contractRepository;
    }

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("🔧 Stripe Configuration:");
        log.info("   - API Key: {}", stripeApiKey != null && !stripeApiKey.isEmpty() ? "✓ Configurée" : "✗ NON CONFIGURÉE");
        log.info("   - Webhook Secret: {}", webhookSecret != null && !webhookSecret.isEmpty() ? "✓ Configuré" : "✗ NON CONFIGURÉ");
        log.info("========================================");
    }

    @PostMapping("/create-payment-intent/{contractId}")
    public ResponseEntity<Map<String, Object>> createPaymentIntent(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails currentUser) throws StripeException {

        try {
            log.info("📝 Création d'un PaymentIntent pour le contrat {}", contractId);

            paymentService.getPaymentsByContractId(contractId, currentUser.getUsername());

            InsuranceContract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

            if (contract.getStatus() == org.example.projet_pi.entity.ContractStatus.COMPLETED) {
                throw new RuntimeException("Ce contrat est déjà complété");
            }

            if (contract.getRemainingAmount() <= 0) {
                throw new RuntimeException("Aucun montant restant à payer pour ce contrat");
            }

            double installmentAmount = contract.calculateInstallmentAmount();

            if (installmentAmount > contract.getRemainingAmount()) {
                installmentAmount = contract.getRemainingAmount();
            }

            long amountInCents = (long) (installmentAmount * 100);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("contractId", contractId.toString());
            metadata.put("clientEmail", contract.getClient().getEmail());
            metadata.put("paymentType", "INSTALLMENT");
            metadata.put("installmentAmount", String.valueOf(installmentAmount));
            metadata.put("remainingAmount", String.valueOf(contract.getRemainingAmount()));

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putAllMetadata(metadata)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", intent.getClientSecret());
            response.put("paymentIntentId", intent.getId());
            response.put("contractId", contractId);
            response.put("amount", installmentAmount);
            response.put("remainingAmount", contract.getRemainingAmount());
            response.put("status", intent.getStatus());

            log.info("✅ PaymentIntent créé: {} pour contrat {}: {} DT",
                    intent.getId(), contractId, installmentAmount);

            return ResponseEntity.ok(response);

        } catch (AccessDeniedException e) {
            log.error("❌ Accès refusé: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création du PaymentIntent: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/create-payment-intent/{contractId}/custom")
    public ResponseEntity<Map<String, Object>> createCustomPaymentIntent(
            @PathVariable Long contractId,
            @RequestBody Map<String, Double> request,
            @AuthenticationPrincipal UserDetails currentUser) throws StripeException {

        try {
            log.info("📝 Création d'un PaymentIntent personnalisé pour le contrat {}", contractId);

            paymentService.getPaymentsByContractId(contractId, currentUser.getUsername());

            Double amount = request.get("amount");
            if (amount == null || amount <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le montant est requis et doit être > 0"));
            }

            InsuranceContract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

            if (contract.getStatus() == org.example.projet_pi.entity.ContractStatus.COMPLETED) {
                throw new RuntimeException("Ce contrat est déjà complété");
            }

            if (amount > contract.getRemainingAmount()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Le montant dépasse le reste à payer",
                        "remainingAmount", contract.getRemainingAmount()
                ));
            }

            long amountInCents = (long) (amount * 100);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("contractId", contractId.toString());
            metadata.put("clientEmail", contract.getClient().getEmail());
            metadata.put("paymentType", "CUSTOM");
            metadata.put("requestedAmount", String.valueOf(amount));
            metadata.put("remainingAmount", String.valueOf(contract.getRemainingAmount()));

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putAllMetadata(metadata)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", intent.getClientSecret());
            response.put("paymentIntentId", intent.getId());
            response.put("contractId", contractId);
            response.put("amount", amount);
            response.put("remainingAmount", contract.getRemainingAmount());
            response.put("status", intent.getStatus());

            log.info("✅ PaymentIntent personnalisé créé: {} pour contrat {}: {} DT",
                    intent.getId(), contractId, amount);

            return ResponseEntity.ok(response);

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/payment-status/{paymentIntentId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(
            @PathVariable String paymentIntentId,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            log.info("📊 Vérification du statut du paiement: {}", paymentIntentId);

            Map<String, Object> status = paymentService.getPaymentIntentStatus(paymentIntentId);

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification du statut: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cancel-payment/{paymentIntentId}")
    public ResponseEntity<Map<String, Object>> cancelPaymentIntent(
            @PathVariable String paymentIntentId,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            log.info("🗑️ Annulation du PaymentIntent: {}", paymentIntentId);

            boolean cancelled = paymentService.cancelStripePaymentIntent(paymentIntentId);

            if (cancelled) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "PaymentIntent annulé avec succès",
                        "paymentIntentId", paymentIntentId
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "success", false,
                        "message", "Impossible d'annuler ce PaymentIntent"
                ));
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'annulation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        log.info("========================================");
        log.info("🔔 WEBHOOK RECEIVED");
        log.info("Signature header present: {}", sigHeader != null);
        log.info("Payload length: {}", payload.length());
        log.info("========================================");

        try {
            if (sigHeader == null || sigHeader.isEmpty()) {
                log.warn("⚠️ No Stripe signature - TEST MODE");
                return handleWebhookTestMode(payload);
            }

            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("✅ Signature verified for event: {}", event.getType());

            return handleEvent(event, payload);

        } catch (SignatureVerificationException e) {
            log.error("❌ Invalid signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        } catch (Exception e) {
            log.error("❌ Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    private ResponseEntity<String> handleWebhookTestMode(String payload) {
        try {
            JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
            String eventType = jsonObject.get("type").getAsString();

            log.info("Event type: {}", eventType);

            if ("payment_intent.succeeded".equals(eventType)) {
                JsonObject dataObject = jsonObject.getAsJsonObject("data");
                JsonObject objectObject = dataObject.getAsJsonObject("object");

                String paymentIntentId = objectObject.get("id").getAsString();
                long amountInCents = objectObject.get("amount").getAsLong();

                JsonObject metadata = objectObject.getAsJsonObject("metadata");
                Long contractId = null;
                if (metadata != null && metadata.has("contractId")) {
                    contractId = Long.parseLong(metadata.get("contractId").getAsString());
                }

                log.info("✅ Payment succeeded (TEST MODE)!");
                log.info("   PaymentIntent ID: {}", paymentIntentId);
                log.info("   Amount: {} DT", amountInCents / 100.0);
                log.info("   Contract ID: {}", contractId);

                if (contractId != null) {
                    paymentService.handleSuccessfulPayment(paymentIntentId, amountInCents, contractId);
                    log.info("✅ Payment processed successfully!");
                } else {
                    log.warn("⚠️ No contractId in metadata - cannot process payment");
                }
            }

            return ResponseEntity.ok("OK (test mode)");

        } catch (Exception e) {
            log.error("Error in test mode: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    private ResponseEntity<String> handleEvent(Event event, String payload) {
        try {
            JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
            JsonObject dataObject = jsonObject.getAsJsonObject("data");
            JsonObject objectObject = dataObject.getAsJsonObject("object");

            JsonObject metadata = objectObject.getAsJsonObject("metadata");
            if (metadata == null || !metadata.has("contractId")) {
                log.error("❌ contractId manquant dans metadata");
                return ResponseEntity.badRequest().body("Contract ID missing");
            }

            Long contractId = Long.parseLong(metadata.get("contractId").getAsString());
            String paymentIntentId = objectObject.get("id").getAsString();
            long amountInCents = objectObject.get("amount").getAsLong();

            log.info("📊 Webhook - ContractId: {}, PaymentIntentId: {}, Amount: {} centimes",
                    contractId, paymentIntentId, amountInCents);

            switch (event.getType()) {
                case "payment_intent.succeeded":
                    log.info("✅ Paiement réussi pour le contrat {}", contractId);
                    paymentService.handleSuccessfulPayment(paymentIntentId, amountInCents, contractId);
                    break;

                case "payment_intent.payment_failed":
                    log.error("❌ Paiement échoué pour le contrat {}", contractId);
                    break;

                default:
                    log.info("ℹ️ Événement non géré: {}", event.getType());
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error handling event: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Stripe Payment Service");
        health.put("webhookSecretConfigured", webhookSecret != null && !webhookSecret.isEmpty());

        return ResponseEntity.ok(health);
    }
}