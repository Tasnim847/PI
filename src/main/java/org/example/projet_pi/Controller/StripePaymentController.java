package org.example.projet_pi.Controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Service.PaymentService;
import org.example.projet_pi.entity.InsuranceContract;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payments/stripe")
@AllArgsConstructor
public class StripePaymentController {

    private final PaymentService paymentService;
    private final Gson gson = new Gson();
    private final InsuranceContractRepository contractRepository;

    @PostMapping("/create-payment-intent/{contractId}")
    public Map<String, String> createPaymentIntent(@PathVariable Long contractId) throws StripeException {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        // Montant de la tranche (installment)
        double installmentAmount = contract.calculateInstallmentAmount();
        long amountInCents = (long) (installmentAmount * 100);

        // Metadata obligatoire pour le webhook
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

        // 🔹 LOG pour vérifier
        log.info("🎯 PaymentIntent créé avec metadata: {}", intent.getMetadata());

        Map<String, String> response = new HashMap<>();
        response.put("clientSecret", intent.getClientSecret());
        response.put("contractId", contractId.toString());
        response.put("amount", String.valueOf(installmentAmount));
        return response;
    }

    @PostMapping("/create-payment-intent/{contractId}/custom")
    public Map<String, String> createCustomPaymentIntent(
            @PathVariable Long contractId,
            @RequestBody Map<String, Double> request) throws StripeException {

        Double amount = request.get("amount");
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Le montant est requis et doit être > 0");
        }

        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        if (amount > contract.getRemainingAmount()) {
            throw new RuntimeException("Le montant dépasse le reste à payer du contrat");
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

        log.info("🎯 PaymentIntent personnalisé créé avec metadata: {}", intent.getMetadata());

        Map<String, String> response = new HashMap<>();
        response.put("clientSecret", intent.getClientSecret());
        response.put("contractId", contractId.toString());
        response.put("amount", String.valueOf(amount));
        return response;
    }

    @PostMapping("/webhook")
    public String handleWebhook(@RequestBody String payload,
                                @RequestHeader("Stripe-Signature") String sigHeader) {
        String endpointSecret = "whsec_3a6d8e127e97f4e5e1534bae05fd570a220df95e3e0efc0cfd5046db239e7f55";

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            log.info("📦 Événement Stripe reçu: {} [{}]", event.getType(), event.getId());

            JsonObject object = JsonParser.parseString(payload)
                    .getAsJsonObject()
                    .getAsJsonObject("data")
                    .getAsJsonObject("object");

            JsonObject metadata = object.getAsJsonObject("metadata");
            if (metadata == null || !metadata.has("contractId")) {
                log.error("❌ contractId manquant dans metadata");
                return "⚠️ Erreur: contractId absent";
            }

            Long contractId = Long.parseLong(metadata.get("contractId").getAsString());
            String paymentIntentId = object.get("id").getAsString();
            long amountInCents = object.get("amount").getAsLong();

            switch (event.getType()) {
                case "payment_intent.succeeded":
                    log.info("✅ Paiement réussi pour le contrat {}", contractId);
                    paymentService.handleSuccessfulPayment(paymentIntentId, amountInCents, contractId);
                    break;

                case "payment_intent.payment_failed":
                    log.error("❌ Paiement échoué pour le contrat {}", contractId);
                    break;

                case "payment_intent.created":   // <--- ajouter ce cas
                    log.info("📌 PaymentIntent créé pour contractId={}, montant={} centimes, type={}",
                            contractId,
                            amountInCents,
                            metadata.get("paymentType").getAsString());
                    // 🔹 Ici tu peux appeler une méthode si tu veux faire autre chose
                    // ex: paymentService.handlePaymentIntentCreated(contractId, paymentIntentId, amountInCents);
                    break;

                default:
                    log.info("ℹ️ Événement non géré: {}", event.getType());
            }

            return "OK";

        } catch (Exception e) {
            log.error("❌ Erreur webhook Stripe:", e);
            return "⚠️ Erreur: " + e.getMessage();
        }
    }

    private void handlePaymentIntentSucceeded(String payload) {
        try {
            // 🔥 SOLUTION: Parser directement le payload JSON
            JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
            JsonObject dataObject = jsonObject.getAsJsonObject("data");
            JsonObject objectObject = dataObject.getAsJsonObject("object");

            // Extraire les informations nécessaires
            String paymentIntentId = objectObject.get("id").getAsString();
            long amount = objectObject.get("amount").getAsLong();

            // Récupérer les metadata
            JsonObject metadataObject = objectObject.getAsJsonObject("metadata");

            if (metadataObject == null || !metadataObject.has("contractId")) {
                log.error("❌ contractId manquant dans metadata");
                return;
            }

            String contractIdStr = metadataObject.get("contractId").getAsString();

            log.info("✅ PaymentIntent réussi: {}", paymentIntentId);
            log.info("💰 Montant: {} centimes", amount);
            log.info("📊 ContractId: {}", contractIdStr);

            if (contractIdStr == null || contractIdStr.isEmpty()) {
                log.error("❌ Pas d'ID de contrat dans les metadata");
                return;
            }

            Long contractId = Long.parseLong(contractIdStr);

            // 🔥 Appeler le service
            paymentService.handleSuccessfulPayment(
                    paymentIntentId,
                    amount,
                    contractId
            );

            log.info("✅ Paiement enregistré avec succès pour le contrat {}", contractId);

        } catch (Exception e) {
            log.error("❌ Erreur dans handlePaymentIntentSucceeded", e);
        }
    }

    private void handlePaymentIntentFailed(String payload) {
        try {
            JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
            JsonObject dataObject = jsonObject.getAsJsonObject("data");
            JsonObject objectObject = dataObject.getAsJsonObject("object");

            String paymentIntentId = objectObject.get("id").getAsString();

            JsonObject metadataObject = objectObject.getAsJsonObject("metadata");
            String contractIdStr = metadataObject != null ? metadataObject.get("contractId").getAsString() : null;

            log.error("❌ PaymentIntent échoué: {}", paymentIntentId);

            if (contractIdStr != null) {
                log.error("📝 Échec de paiement pour le contrat {}", contractIdStr);
            }
        } catch (Exception e) {
            log.error("❌ Erreur dans handlePaymentIntentFailed", e);
        }
    }
}