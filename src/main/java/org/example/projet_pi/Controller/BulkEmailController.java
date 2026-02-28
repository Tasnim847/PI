package org.example.projet_pi.Controller;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Service.BulkEmailService;
import org.example.projet_pi.Service.EmailService;
import org.example.projet_pi.entity.*;
import org.example.projet_pi.Repository.ClientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/bulk-email")
@RequiredArgsConstructor
public class BulkEmailController {

    private final BulkEmailService bulkEmailService;
    private final ClientRepository clientRepository;
    private final InsuranceContractRepository contractRepository;
    private final EmailService emailService;

    /**
     * Envoyer des rappels à TOUS les clients
     */
    @PostMapping("/send-reminders")
    public ResponseEntity<Map<String, Object>> sendRemindersToAll() {
        Map<String, Object> response = new HashMap<>();

        try {
            bulkEmailService.sendRemindersToAllClients();

            response.put("success", true);
            response.put("message", "Rappels envoyés à tous les clients");
            response.put("timestamp", new Date());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * FORCER l'envoi à TOUS les clients (même sans condition)
     */
    @PostMapping("/force-send-all")
    public ResponseEntity<Map<String, Object>> forceSendToAll() {
        Map<String, Object> response = new HashMap<>();
        List<Client> allClients = clientRepository.findAll();
        int sentCount = 0;

        try {
            for (Client client : allClients) {
                if (client.getEmail() != null && !client.getEmail().isEmpty()) {
                    List<InsuranceContract> contracts = contractRepository.findByClient(client);

                    for (InsuranceContract contract : contracts) {
                        if (contract.getStatus() == ContractStatus.ACTIVE) {
                            List<Payment> pendingPayments = contract.getPayments().stream()
                                    .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                                    .toList();

                            for (Payment payment : pendingPayments) {
                                emailService.sendPaymentReminderEmail(
                                        client, contract, payment, 3
                                );
                                sentCount++;
                            }
                        }
                    }
                }
            }

            response.put("success", true);
            response.put("message", "Emails forcés envoyés");
            response.put("emailsSent", sentCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 🔔 NOUVELLE MÉTHODE : Envoyer les rappels automatiques (J-3, J-1, J0)
     * Cette méthode applique les règles :
     * - J-3 : Envoi 3 jours avant
     * - J-1 : Envoi la veille
     * - J0 : Envoi le jour même
     */
    @PostMapping("/send-auto-reminders")
    public ResponseEntity<Map<String, Object>> sendAutoReminders() {
        Map<String, Object> response = new HashMap<>();
        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        Date today = new Date();

        int rappelsJ3 = 0;
        int rappelsJ1 = 0;
        int rappelsJ0 = 0;
        int totalClients = 0;

        try {
            for (InsuranceContract contract : activeContracts) {
                Client client = contract.getClient();
                if (client == null || client.getEmail() == null || client.getEmail().isEmpty()) {
                    continue;
                }

                totalClients++;

                List<Payment> pendingPayments = contract.getPayments().stream()
                        .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                        .toList();

                for (Payment payment : pendingPayments) {
                    long daysUntil = TimeUnit.DAYS.convert(
                            payment.getPaymentDate().getTime() - today.getTime(),
                            TimeUnit.MILLISECONDS
                    );

                    if (daysUntil == 3) {
                        emailService.sendPaymentReminderEmail(client, contract, payment, 3);
                        rappelsJ3++;

                    } else if (daysUntil == 1) {
                        emailService.sendPaymentReminderEmail(client, contract, payment, 1);
                        rappelsJ1++;

                    } else if (daysUntil == 0) {
                        emailService.sendPaymentReminderEmail(client, contract, payment, 0);
                        rappelsJ0++;
                    }
                }
            }

            response.put("success", true);
            response.put("message", "Rappels automatiques envoyés");
            response.put("timestamp", new Date());
            response.put("stats", Map.of(
                    "rappelsJ3", rappelsJ3,
                    "rappelsJ1", rappelsJ1,
                    "rappelsJ0", rappelsJ0,
                    "totalClients", totalClients
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 🔔 NOUVELLE MÉTHODE : Vérifier les paiements du jour
     */
    @GetMapping("/check-today-payments")
    public ResponseEntity<Map<String, Object>> checkTodayPayments() {
        Map<String, Object> response = new HashMap<>();
        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        Date today = new Date();

        List<Map<String, Object>> paymentsToday = new java.util.ArrayList<>();
        List<Map<String, Object>> paymentsTomorrow = new java.util.ArrayList<>();
        List<Map<String, Object>> paymentsIn3Days = new java.util.ArrayList<>();

        try {
            for (InsuranceContract contract : activeContracts) {
                Client client = contract.getClient();
                if (client == null) continue;

                for (Payment payment : contract.getPayments()) {
                    if (payment.getStatus() != PaymentStatus.PENDING) continue;

                    long daysUntil = TimeUnit.DAYS.convert(
                            payment.getPaymentDate().getTime() - today.getTime(),
                            TimeUnit.MILLISECONDS
                    );

                    Map<String, Object> paymentInfo = new HashMap<>();
                    paymentInfo.put("client", client.getEmail());
                    paymentInfo.put("clientName", client.getFirstName() + " " + client.getLastName());
                    paymentInfo.put("contractId", contract.getContractId());
                    paymentInfo.put("amount", payment.getAmount());
                    paymentInfo.put("dueDate", payment.getPaymentDate());

                    if (daysUntil == 0) {
                        paymentsToday.add(paymentInfo);
                    } else if (daysUntil == 1) {
                        paymentsTomorrow.add(paymentInfo);
                    } else if (daysUntil == 3) {
                        paymentsIn3Days.add(paymentInfo);
                    }
                }
            }

            response.put("success", true);
            response.put("paymentsToday", paymentsToday);
            response.put("paymentsTomorrow", paymentsTomorrow);
            response.put("paymentsIn3Days", paymentsIn3Days);
            response.put("counts", Map.of(
                    "today", paymentsToday.size(),
                    "tomorrow", paymentsTomorrow.size(),
                    "in3Days", paymentsIn3Days.size()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 🔔 NOUVELLE MÉTHODE : Configuration des rappels automatiques
     */
    @GetMapping("/reminders-config")
    public ResponseEntity<Map<String, Object>> getRemindersConfig() {
        Map<String, Object> config = new HashMap<>();

        config.put("rappelJ3", "📧 Envoyé 3 jours avant l'échéance");
        config.put("rappelJ1", "⚠️ Envoyé la veille de l'échéance");
        config.put("rappelJ0", "🚨 Envoyé le jour même de l'échéance");
        config.put("horaireEnvoi", "Tous les jours à 8h00 (automatique)");
        config.put("endpoints", Map.of(
                "sendAutoReminders", "POST /api/bulk-email/send-auto-reminders",
                "checkTodayPayments", "GET /api/bulk-email/check-today-payments",
                "remindersConfig", "GET /api/bulk-email/reminders-config"
        ));

        return ResponseEntity.ok(config);
    }

    /**
     * Envoyer une newsletter à tous les clients
     */
    @PostMapping("/newsletter")
    public ResponseEntity<Map<String, Object>> sendNewsletter(
            @RequestParam String subject,
            @RequestParam String message) {

        Map<String, Object> response = new HashMap<>();

        try {
            bulkEmailService.sendEmailToAllClients(subject, message);

            response.put("success", true);
            response.put("message", "Newsletter envoyée à tous les clients");
            response.put("subject", subject);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Liste tous les clients avec leurs emails
     */
    @GetMapping("/clients")
    public ResponseEntity<List<Client>> getAllClients() {
        List<Client> clients = clientRepository.findAll();
        return ResponseEntity.ok(clients);
    }

    /**
     * Statistiques des emails par domaine
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEmailStats() {
        List<Client> clients = clientRepository.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClients", clients.size());

        long clientsWithEmail = clients.stream()
                .filter(c -> c.getEmail() != null && !c.getEmail().isEmpty())
                .count();
        stats.put("clientsWithEmail", clientsWithEmail);

        // Statistiques par domaine
        Map<String, Integer> domainCount = new HashMap<>();
        for (Client client : clients) {
            if (client.getEmail() != null && !client.getEmail().isEmpty()) {
                String domain = client.getEmail().split("@")[1];
                domainCount.put(domain, domainCount.getOrDefault(domain, 0) + 1);
            }
        }
        stats.put("domains", domainCount);

        return ResponseEntity.ok(stats);
    }

    /**
     * Tester l'envoi à un email spécifique
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testSingleEmail(
            @RequestParam String email) {

        Map<String, Object> response = new HashMap<>();

        try {
            Client client = clientRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));

            List<InsuranceContract> contracts = contractRepository.findByClient(client);

            if (!contracts.isEmpty()) {
                InsuranceContract contract = contracts.get(0);
                Payment payment = contract.getPayments().stream()
                        .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                        .findFirst()
                        .orElse(null);

                if (payment != null) {
                    emailService.sendPaymentReminderEmail(client, contract, payment, 3);
                    response.put("success", true);
                    response.put("message", "Email test envoyé à " + email);
                } else {
                    response.put("success", false);
                    response.put("message", "Aucun paiement en attente");
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}