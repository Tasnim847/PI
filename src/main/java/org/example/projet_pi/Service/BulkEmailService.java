package org.example.projet_pi.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.InsuranceContract;
import org.example.projet_pi.entity.Payment;
import org.example.projet_pi.entity.ContractStatus;
import org.example.projet_pi.entity.PaymentStatus;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkEmailService {

    private final ClientRepository clientRepository;
    private final InsuranceContractRepository contractRepository;
    private final EmailService emailService;
    private final VonageSmsService smsService;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * MÉTHODE PRINCIPALE : Envoie les rappels selon les règles :
     * - J-3 : Rappel 3 jours avant
     * - J-1 : Rappel la veille
     * - J0 : Rappel le jour même (paiement aujourd'hui)
     *
     * Exécutée automatiquement tous les jours à 8h00
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDefaultReminders() {
        log.info("🔔 DÉBUT - Envoi automatique des rappels de paiement - {}", new Date());

        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        Date today = new Date();

        int rappelsJ3 = 0;
        int rappelsJ1 = 0;
        int rappelsJ0 = 0;
        int totalClients = 0;

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
                    log.info("📧 J-3: Rappel envoyé à {} pour le contrat {}",
                            client.getEmail(), contract.getContractId());
                } else if (daysUntil == 1) {
                    emailService.sendPaymentReminderEmail(client, contract, payment, 1);
                    rappelsJ1++;
                    log.info("📧 J-1: Rappel envoyé à {} pour le contrat {}",
                            client.getEmail(), contract.getContractId());
                } else if (daysUntil == 0) {
                    emailService.sendPaymentReminderEmail(client, contract, payment, 0);
                    rappelsJ0++;
                    log.info("📧 J0: RAPPEL URGENT - Paiement aujourd'hui pour {} contrat {}",
                            client.getEmail(), contract.getContractId());
                }
            }
        }

        log.info("🔔 FIN - Résumé des envois automatiques:");
        log.info("   - Rappels J-3 (3 jours avant): {}", rappelsJ3);
        log.info("   - Rappels J-1 (veille): {}", rappelsJ1);
        log.info("   - Rappels J0 (aujourd'hui): {}", rappelsJ0);
        log.info("   - Total clients traités: {}", totalClients);
    }

    /**
     * Méthode pour envoyer des rappels à TOUS les clients (sans condition de date)
     */
    public void sendRemindersToAllClients() {
        log.info("📧 Envoi de rappels à TOUS les clients - {}", new Date());

        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        int totalEmailsSent = 0;

        for (InsuranceContract contract : activeContracts) {
            Client client = contract.getClient();
            if (client == null || client.getEmail() == null || client.getEmail().isEmpty()) {
                continue;
            }

            List<Payment> pendingPayments = contract.getPayments().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                    .toList();

            for (Payment payment : pendingPayments) {
                emailService.sendPaymentReminderEmail(client, contract, payment, 3);
                totalEmailsSent++;
                log.info("📧 Rappel envoyé à {} pour le contrat {}",
                        client.getEmail(), contract.getContractId());
            }
        }

        log.info("📧 Total emails envoyés: {}", totalEmailsSent);
    }

    /**
     * Méthode manuelle pour déclencher l'envoi automatique (J-3, J-1, J0)
     */
    public void sendManualReminders() {
        log.info("📧 Envoi MANUEL des rappels automatiques - {}", new Date());
        sendDefaultReminders();
    }

    /**
     * Vérifier les paiements du jour
     */
    public Map<String, Object> checkTodayPayments() {
        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        Date today = new Date();

        int todayCount = 0;
        int tomorrowCount = 0;
        int in3DaysCount = 0;

        log.info("📅 Vérification des paiements - {}", today);

        for (InsuranceContract contract : activeContracts) {
            Client client = contract.getClient();
            if (client == null) continue;

            for (Payment payment : contract.getPayments()) {
                if (payment.getStatus() != PaymentStatus.PENDING) continue;

                long daysUntil = TimeUnit.DAYS.convert(
                        payment.getPaymentDate().getTime() - today.getTime(),
                        TimeUnit.MILLISECONDS
                );

                if (daysUntil == 0) {
                    todayCount++;
                    log.info("⚠️ AUJOURD'HUI - {} - Contrat {} - {} TND",
                            client.getEmail(), contract.getContractId(), payment.getAmount());
                } else if (daysUntil == 1) {
                    tomorrowCount++;
                    log.info("📅 DEMAIN - {} - Contrat {} - {} TND",
                            client.getEmail(), contract.getContractId(), payment.getAmount());
                } else if (daysUntil == 3) {
                    in3DaysCount++;
                    log.info("🔔 J-3 - {} - Contrat {} - {} TND",
                            client.getEmail(), contract.getContractId(), payment.getAmount());
                }
            }
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("today", todayCount);
        result.put("tomorrow", tomorrowCount);
        result.put("in3Days", in3DaysCount);

        return result;
    }

    /**
     * Envoyer une newsletter à tous les clients
     */
    public void sendEmailToAllClients(String subject, String messageContent) {
        log.info("📧 Début envoi newsletter: '{}' - {}", subject, new Date());

        List<Client> allClients = clientRepository.findAll();
        int sentCount = 0;
        int failedCount = 0;

        for (Client client : allClients) {
            if (client.getEmail() != null && !client.getEmail().isEmpty()) {
                try {
                    sendNewsletter(client, subject, messageContent);
                    sentCount++;
                    log.info("✅ Newsletter envoyée à {}", client.getEmail());
                    Thread.sleep(100);
                } catch (Exception e) {
                    failedCount++;
                    log.error("❌ Erreur pour {}: {}", client.getEmail(), e.getMessage());
                }
            }
        }

        log.info("📧 Newsletter terminée: {} envoyés, {} échecs", sentCount, failedCount);
    }

    /**
     * Envoyer une newsletter à un client spécifique
     */
    public void sendNewsletterToClient(Long clientId, String subject, String messageContent) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        sendNewsletter(client, subject, messageContent);
        log.info("📧 Newsletter envoyée à {}", client.getEmail());
    }

    /**
     * Obtenir les statistiques des emails
     */
    public Map<String, Object> getEmailStatistics() {
        List<Client> allClients = clientRepository.findAll();

        long totalWithEmail = allClients.stream()
                .filter(c -> c.getEmail() != null && !c.getEmail().isEmpty())
                .count();

        Map<String, Integer> domainCount = new java.util.HashMap<>();
        for (Client client : allClients) {
            if (client.getEmail() != null && !client.getEmail().isEmpty()) {
                String domain = client.getEmail().split("@")[1];
                domainCount.put(domain, domainCount.getOrDefault(domain, 0) + 1);
            }
        }

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalClients", allClients.size());
        stats.put("clientsWithEmail", totalWithEmail);
        stats.put("clientsWithoutEmail", allClients.size() - totalWithEmail);
        stats.put("domains", domainCount);

        return stats;
    }

    /**
     * 📧 Envoyer une newsletter à un client (méthode interne)
     */
    private void sendNewsletter(Client client, String subject, String messageContent) {
        try {
            if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
                log.error("❌ Email client manquant pour {}", client.getFirstName());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(client.getEmail());
            helper.setSubject(subject);

            // Préparer le contexte Thymeleaf
            Context context = new Context();
            context.setVariable("clientName", client.getFirstName() + " " + client.getLastName());
            context.setVariable("clientEmail", client.getEmail());
            context.setVariable("messageContent", messageContent);
            context.setVariable("currentDate", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

            String htmlContent = templateEngine.process("newsletter", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("✅ Newsletter envoyée à {}", client.getEmail());

        } catch (MessagingException e) {
            log.error("❌ Erreur envoi newsletter à {}: {}", client.getEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur inattendue: {}", e.getMessage());
        }
    }

    /**
     * Méthode utilitaire pour envoyer SMS
     */
    private void sendSmsIfAvailable(Client client, String message) {
        try {
            if (client.getTelephone() != null && !client.getTelephone().trim().isEmpty()) {
                smsService.sendSms(client.getTelephone(), message);
                log.info("✅ SMS newsletter envoyé à {}", client.getTelephone());
            }
        } catch (Exception e) {
            log.error("❌ Erreur envoi SMS newsletter: {}", e.getMessage());
        }
    }
}