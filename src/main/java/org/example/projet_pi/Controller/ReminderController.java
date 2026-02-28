package org.example.projet_pi.Controller;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Service.EmailService;
import org.example.projet_pi.Service.PaymentService;
import org.example.projet_pi.entity.InsuranceContract;
import org.example.projet_pi.entity.Payment;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final PaymentService paymentService;
    private final EmailService emailService;
    private final InsuranceContractRepository contractRepository;

    /**
     * Tester l'envoi d'un rappel pour un contrat spécifique
     */
    @PostMapping("/test/{contractId}/{daysBefore}")
    public ResponseEntity<Map<String, Object>> testReminder(
            @PathVariable Long contractId,
            @PathVariable int daysBefore,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();

        try {
            InsuranceContract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

            // Trouver le prochain paiement en attente
            Payment nextPayment = contract.getPayments().stream()
                    .filter(p -> p.getStatus() == org.example.projet_pi.entity.PaymentStatus.PENDING)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Aucun paiement en attente"));

            // Envoyer l'email
            emailService.sendPaymentReminderEmail(
                    contract.getClient(),
                    contract,
                    nextPayment,
                    daysBefore
            );

            response.put("success", true);
            response.put("message", "Email de test envoyé avec succès");
            response.put("contractId", contractId);
            response.put("client", contract.getClient().getEmail());
            response.put("daysBefore", daysBefore);
            response.put("paymentAmount", nextPayment.getAmount());
            response.put("paymentDate", nextPayment.getPaymentDate());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lancer la vérification manuelle des rappels
     */
    @PostMapping("/check-all")
    public ResponseEntity<Map<String, Object>> checkAllReminders() {
        Map<String, Object> response = new HashMap<>();

        try {
            paymentService.checkUpcomingPayments();

            response.put("success", true);
            response.put("message", "Vérification des rappels lancée");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Vérifier les rappels pour un contrat spécifique
     */
    @PostMapping("/check-contract/{contractId}")
    public ResponseEntity<Map<String, Object>> checkContractReminders(
            @PathVariable Long contractId) {

        Map<String, Object> response = new HashMap<>();

        try {
            int remindersSent = paymentService.checkAndSendRemindersForContract(contractId);

            response.put("success", true);
            response.put("contractId", contractId);
            response.put("remindersSent", remindersSent);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Récupérer les prochains paiements avec leurs rappels
     */
    @GetMapping("/upcoming/{contractId}")
    public ResponseEntity<Map<String, Object>> getUpcomingPayments(
            @PathVariable Long contractId) {

        Map<String, Object> response = new HashMap<>();

        try {
            InsuranceContract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

            List<Map<String, Object>> upcomingPayments = contract.getPayments().stream()
                    .filter(p -> p.getStatus() == org.example.projet_pi.entity.PaymentStatus.PENDING)
                    .map(p -> {
                        Map<String, Object> paymentInfo = new HashMap<>();
                        paymentInfo.put("paymentId", p.getPaymentId());
                        paymentInfo.put("amount", p.getAmount());
                        paymentInfo.put("dueDate", p.getPaymentDate());

                        // Calculer les jours restants
                        long daysUntil = (p.getPaymentDate().getTime() - new Date().getTime())
                                / (1000 * 60 * 60 * 24);
                        paymentInfo.put("daysUntil", daysUntil);

                        // Déterminer si un rappel est nécessaire
                        boolean needsReminder = daysUntil == 30 || daysUntil == 15 ||
                                daysUntil == 7 || daysUntil == 3 || daysUntil == 1;
                        paymentInfo.put("needsReminder", needsReminder);

                        return paymentInfo;
                    })
                    .toList();

            response.put("success", true);
            response.put("contractId", contractId);
            response.put("upcomingPayments", upcomingPayments);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}