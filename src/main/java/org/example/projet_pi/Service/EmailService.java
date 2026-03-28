package org.example.projet_pi.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.entity.*;
import org.example.projet_pi.Repository.PaymentReminderRepository;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.net.ssl.SSLHandshakeException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final PaymentReminderRepository reminderRepository;
    private final VonageSmsService smsService;  // 🔥 SEUL AJOUT

    // ============================================================
    // 🔥 NOUVELLE MÉTHODE PRIVÉE POUR ENVOYER SMS (SANS MODIFIER L'EXISTANT)
    // ============================================================

    private void sendSmsIfAvailable(Client client, String message) {
        try {
            if (client.getTelephone() != null && !client.getTelephone().trim().isEmpty()) {
                smsService.sendSms(client.getTelephone(), message);
                log.info("✅ SMS envoyé à {}", client.getTelephone());
            }
        } catch (Exception e) {
            log.error("❌ Erreur envoi SMS: {}", e.getMessage());
        }
    }

    // ============================================================
    // 🔥 RAPPEL DE PAIEMENT AVEC SMS AJOUTÉ
    // ============================================================

    @Async
    public void sendPaymentReminderEmail(Client client, InsuranceContract contract, Payment payment, int daysBefore) {
        try {
            // Vérifier si un rappel a déjà été envoyé aujourd'hui
            if (hasReminderBeenSentToday(payment, daysBefore)) {
                log.info("Rappel déjà envoyé aujourd'hui pour le paiement {} (J-{})", payment.getPaymentId(), daysBefore);
                return;
            }

            // Valider l'email du client
            if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
                log.error("Email client manquant pour le contrat {}", contract.getContractId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(client.getEmail());
            helper.setSubject(getSubjectByDaysBefore(daysBefore));

            // Préparer le contexte Thymeleaf
            Context context = new Context();
            context.setVariable("clientName", client.getFirstName() + " " + client.getLastName());
            context.setVariable("contractId", contract.getContractId());
            context.setVariable("paymentAmount", String.format("%.3f", payment.getAmount()));
            context.setVariable("paymentDate", new SimpleDateFormat("dd/MM/yyyy").format(payment.getPaymentDate()));
            context.setVariable("daysBefore", daysBefore);
            context.setVariable("remainingAmount", String.format("%.3f", contract.getRemainingAmount()));
            context.setVariable("totalPaid", String.format("%.3f", contract.getTotalPaid()));
            context.setVariable("urgent", daysBefore <= 3);

            String htmlContent = templateEngine.process("payment-reminder", context);
            helper.setText(htmlContent, true);

            // Tentative d'envoi
            mailSender.send(message);

            // Sauvegarder le rappel
            saveReminder(payment, daysBefore, "SUCCESS");

            log.info("✅ Email envoyé avec succès à {} pour le contrat {} (J-{})",
                    client.getEmail(), contract.getContractId(), daysBefore);

            // 🔥 AJOUT SMS
            String smsMessage;
            if (daysBefore == 0) {
                smsMessage = String.format("URGENT: Paiement de %.2f DT pour contrat %s dû AUJOURD'HUI!",
                        payment.getAmount(), contract.getContractId());
            } else if (daysBefore == 1) {
                smsMessage = String.format("Rappel: Paiement de %.2f DT pour contrat %s DEMAIN.",
                        payment.getAmount(), contract.getContractId());
            } else {
                smsMessage = String.format("Rappel: Paiement de %.2f DT pour contrat %s dans %d jours.",
                        payment.getAmount(), contract.getContractId(), daysBefore);
            }
            sendSmsIfAvailable(client, smsMessage);

        } catch (MailSendException e) {
            log.error("❌ Erreur d'envoi mail à {}: {}", client.getEmail(), e.getMessage());

            // Analyser la cause racine
            Throwable rootCause = getRootCause(e);
            if (rootCause instanceof SSLHandshakeException) {
                log.error("🔐 Problème de certificat SSL. Vérifiez la configuration TLS.");
                log.error("Solution: Ajoutez 'spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com' dans application.properties");
            }

            saveReminder(payment, daysBefore, "FAILED: " + rootCause.getClass().getSimpleName());

        } catch (MessagingException e) {
            log.error("❌ Erreur de création du message pour {}: {}", client.getEmail(), e.getMessage());
            saveReminder(payment, daysBefore, "FAILED: MessagingException");

        } catch (Exception e) {
            log.error("❌ Erreur inattendue pour {}: {}", client.getEmail(), e.getMessage());
            saveReminder(payment, daysBefore, "FAILED: " + e.getClass().getSimpleName());
        }
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause != null) {
            return getRootCause(cause);
        }
        return throwable;
    }

    private String getSubjectByDaysBefore(int daysBefore) {
        switch (daysBefore) {
            case 3:
                return "🔔 Rappel de paiement - Échéance dans 3 jours";
            case 1:
                return "⚠️ URGENT - Paiement DEMAIN";
            case 0:
                return "🚨 URGENT - Paiement AUJOURD'HUI";
            default:
                return "🔔 Rappel de paiement - J" + (daysBefore > 0 ? "-" + daysBefore : " aujourd'hui");
        }
    }

    private boolean hasReminderBeenSentToday(Payment payment, int daysBefore) {
        try {
            return reminderRepository.existsByPaymentAndDaysBeforeAndSentDateBetween(
                    payment, daysBefore, getStartOfDay(), getEndOfDay());
        } catch (Exception e) {
            log.error("Erreur vérification rappel: {}", e.getMessage());
            return false;
        }
    }

    private void saveReminder(Payment payment, int daysBefore, String status) {
        try {
            PaymentReminder reminder = new PaymentReminder();
            reminder.setPayment(payment);
            reminder.setDaysBefore(daysBefore);
            reminder.setSentDate(new Date());
            reminder.setSent("SUCCESS".equals(status));
            reminder.setEmailStatus(status);
            reminderRepository.save(reminder);
        } catch (Exception e) {
            log.error("Erreur sauvegarde rappel: {}", e.getMessage());
        }
    }

    private Date getStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getEndOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }


    /**
     * Envoyer un email à une liste de clients
     */
    @Async
    public void sendBulkReminders(List<Client> clients, InsuranceContract contract, Payment payment, int daysBefore) {
        for (Client client : clients) {
            try {
                sendPaymentReminderEmail(client, contract, payment, daysBefore);
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("Erreur pour {}: {}", client.getEmail(), e.getMessage());
            }
        }
    }

    /**
     * 🎉 Envoyer un email de confirmation d'acceptation du contrat
     */
    @Async
    public void sendContractAcceptedEmail(Client client, InsuranceContract contract) {
        try {
            if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
                log.error("Email client manquant pour le contrat {}", contract.getContractId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(client.getEmail());
            helper.setSubject("✅ Félicitations ! Votre contrat d'assurance a été accepté");

            Context context = new Context();
            context.setVariable("clientName", client.getFirstName() + " " + client.getLastName());
            context.setVariable("contractId", contract.getContractId());
            context.setVariable("startDate", new SimpleDateFormat("dd/MM/yyyy").format(contract.getStartDate()));
            context.setVariable("endDate", new SimpleDateFormat("dd/MM/yyyy").format(contract.getEndDate()));
            context.setVariable("premium", String.format("%.3f", contract.getPremium()));
            context.setVariable("paymentFrequency", contract.getPaymentFrequency() != null ?
                    contract.getPaymentFrequency().toString() : "Mensuel");
            context.setVariable("agentName", contract.getAgentAssurance() != null ?
                    contract.getAgentAssurance().getFirstName() + " " + contract.getAgentAssurance().getLastName() : "Votre agent");
            context.setVariable("agentEmail", contract.getAgentAssurance() != null ?
                    contract.getAgentAssurance().getEmail() : "");
            context.setVariable("currentDate", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

            String htmlContent = templateEngine.process("contract-accepted", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ Email d'acceptation envoyé à {} pour le contrat {}",
                    client.getEmail(), contract.getContractId());

            // 🔥 AJOUT SMS
            String smsMessage = String.format("Félicitations! Contrat %s accepté. Prime: %.2f DT/%s",
                    contract.getContractId(), contract.getPremium(),
                    contract.getPaymentFrequency() != null ? contract.getPaymentFrequency().toString() : "mois");
            sendSmsIfAvailable(client, smsMessage);

        } catch (MessagingException e) {
            log.error("❌ Erreur lors de l'envoi de l'email d'acceptation à {}: {}",
                    client.getEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur inattendue: {}", e.getMessage());
        }
    }

    /**
     * 🚨 Envoyer un email d'annulation de contrat
     */
    @Async
    public void sendContractCancelledEmail(Client client, InsuranceContract contract, int latePaymentCount) {
        try {
            if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
                log.error("Email client manquant pour le contrat {}", contract.getContractId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(client.getEmail());
            helper.setSubject("🚨 Important : Votre contrat d'assurance a été annulé");

            // Préparer le contexte Thymeleaf
            Context context = new Context();
            context.setVariable("clientName", client.getFirstName() + " " + client.getLastName());
            context.setVariable("contractId", contract.getContractId());
            context.setVariable("cancellationDate", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
            context.setVariable("latePaymentCount", latePaymentCount);
            context.setVariable("agentName", contract.getAgentAssurance() != null ?
                    contract.getAgentAssurance().getFirstName() + " " + contract.getAgentAssurance().getLastName() : "Votre agent");
            context.setVariable("agentEmail", contract.getAgentAssurance() != null ?
                    contract.getAgentAssurance().getEmail() : "");

            String htmlContent = templateEngine.process("contract-cancelled", context);
            helper.setText(htmlContent, true);

            // Envoyer l'email
            mailSender.send(message);

            log.info("✅ Email d'annulation envoyé à {} pour le contrat {} ({} retards)",
                    client.getEmail(), contract.getContractId(), latePaymentCount);

            // 🔥 AJOUT SMS
            String smsMessage = String.format("URGENT: Contrat %s annulé (%d retards). Contactez votre agent.",
                    contract.getContractId(), latePaymentCount);
            sendSmsIfAvailable(client, smsMessage);

        } catch (MessagingException e) {
            log.error("❌ Erreur lors de l'envoi de l'email d'annulation à {}: {}",
                    client.getEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur inattendue: {}", e.getMessage());
        }
    }


    /**
     * 📧 Envoyer un email de notification de rejet de contrat
     */
    @Async
    public void sendContractRejectedEmail(Client client, InsuranceContract contract, String rejectionReason) {
        try {
            if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
                log.error("Email client manquant pour le contrat {}", contract.getContractId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(client.getEmail());
            helper.setSubject("❌ Information : Votre contrat d'assurance n'a pas été accepté");

            // Préparer le contexte Thymeleaf
            Context context = new Context();
            context.setVariable("clientName", client.getFirstName() + " " + client.getLastName());
            context.setVariable("contractId", contract.getContractId());
            context.setVariable("rejectionDate", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
            context.setVariable("rejectionReason", rejectionReason != null ? rejectionReason : "Non spécifiée");
            context.setVariable("agentName", contract.getAgentAssurance() != null ?
                    contract.getAgentAssurance().getFirstName() + " " + contract.getAgentAssurance().getLastName() : "Votre agent");
            context.setVariable("agentEmail", contract.getAgentAssurance() != null ?
                    contract.getAgentAssurance().getEmail() : "");

            String htmlContent = templateEngine.process("contract-rejected", context);
            helper.setText(htmlContent, true);

            // Envoyer l'email
            mailSender.send(message);

            log.info("✅ Email de rejet envoyé à {} pour le contrat {} (raison: {})",
                    client.getEmail(), contract.getContractId(), rejectionReason);

            // 🔥 AJOUT SMS
            String shortReason = rejectionReason != null && rejectionReason.length() > 50
                    ? rejectionReason.substring(0, 47) + "..."
                    : rejectionReason;
            String smsMessage = String.format("Contrat %s rejeté. Motif: %s. Contactez votre agent.",
                    contract.getContractId(), shortReason != null ? shortReason : "Non spécifié");
            sendSmsIfAvailable(client, smsMessage);

        } catch (MessagingException e) {
            log.error("❌ Erreur lors de l'envoi de l'email de rejet à {}: {}",
                    client.getEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur inattendue: {}", e.getMessage());
        }
    }


    /**
     * 📧 Envoyer un email de confirmation d'acceptation de claim
     */
    @Async
    public void sendClaimApprovedEmail(Client client, Claim claim, Compensation compensation) {
        try {
            if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
                log.error("Email client manquant pour le claim {}", claim.getClaimId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(client.getEmail());
            helper.setSubject("✅ Votre réclamation a été acceptée - Compensation en cours");

            // Préparer le contexte Thymeleaf
            Context context = new Context();
            context.setVariable("clientName", client.getFirstName() + " " + client.getLastName());
            context.setVariable("claimId", claim.getClaimId());
            context.setVariable("contractId", claim.getContract() != null ? claim.getContract().getContractId() : "N/A");
            context.setVariable("claimDate", new SimpleDateFormat("dd/MM/yyyy").format(claim.getClaimDate()));
            context.setVariable("claimedAmount", String.format("%.3f", claim.getClaimedAmount()));
            context.setVariable("approvedAmount", String.format("%.3f", claim.getApprovedAmount()));

            // Détails de la compensation
            if (compensation != null) {
                context.setVariable("compensationAmount", String.format("%.3f", compensation.getAmount()));
                context.setVariable("paymentDate", new SimpleDateFormat("dd/MM/yyyy").format(compensation.getPaymentDate()));
            } else {
                context.setVariable("compensationAmount", "En cours de traitement");
                context.setVariable("paymentDate", "À déterminer");
            }

            // Calcul de la franchise
            double franchise = claim.getContract() != null ? claim.getContract().getDeductible() : 0;
            double insurancePayment = Math.max(0, claim.getApprovedAmount() - franchise);
            context.setVariable("franchise", String.format("%.3f", franchise));
            context.setVariable("insurancePayment", String.format("%.3f", insurancePayment));

            context.setVariable("agentName", claim.getContract() != null && claim.getContract().getAgentAssurance() != null ?
                    claim.getContract().getAgentAssurance().getFirstName() + " " + claim.getContract().getAgentAssurance().getLastName() : "Votre agent");
            context.setVariable("agentEmail", claim.getContract() != null && claim.getContract().getAgentAssurance() != null ?
                    claim.getContract().getAgentAssurance().getEmail() : "");
            context.setVariable("currentDate", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

            String htmlContent = templateEngine.process("claim-approved", context);
            helper.setText(htmlContent, true);

            // Envoyer l'email
            mailSender.send(message);

            log.info("✅ Email d'acceptation de claim envoyé à {} pour le claim {}",
                    client.getEmail(), claim.getClaimId());

            // 🔥 AJOUT SMS
            String smsMessage = String.format("Réclamation %s acceptée. Compensation: %.2f DT.",
                    claim.getClaimId(), claim.getApprovedAmount());
            sendSmsIfAvailable(client, smsMessage);

        } catch (MessagingException e) {
            log.error("❌ Erreur lors de l'envoi de l'email d'acceptation de claim à {}: {}",
                    client.getEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur inattendue: {}", e.getMessage());
        }
    }

    /**
     * 📧 Envoyer un email de rejet de claim
     */
    @Async
    public void sendClaimRejectedEmail(Client client, Claim claim, String rejectionReason) {
        try {
            if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
                log.error("Email client manquant pour le claim {}", claim.getClaimId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(client.getEmail());
            helper.setSubject("❌ Information concernant votre réclamation");

            // Préparer le contexte Thymeleaf
            Context context = new Context();
            context.setVariable("clientName", client.getFirstName() + " " + client.getLastName());
            context.setVariable("claimId", claim.getClaimId());
            context.setVariable("contractId", claim.getContract() != null ? claim.getContract().getContractId() : "N/A");
            context.setVariable("claimDate", new SimpleDateFormat("dd/MM/yyyy").format(claim.getClaimDate()));
            context.setVariable("claimedAmount", String.format("%.3f", claim.getClaimedAmount()));
            context.setVariable("rejectionReason", rejectionReason != null ? rejectionReason : "Non spécifiée");
            context.setVariable("rejectionDate", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
            context.setVariable("agentName", claim.getContract() != null && claim.getContract().getAgentAssurance() != null ?
                    claim.getContract().getAgentAssurance().getFirstName() + " " + claim.getContract().getAgentAssurance().getLastName() : "Votre agent");
            context.setVariable("agentEmail", claim.getContract() != null && claim.getContract().getAgentAssurance() != null ?
                    claim.getContract().getAgentAssurance().getEmail() : "");

            String htmlContent = templateEngine.process("claim-rejected", context);
            helper.setText(htmlContent, true);

            // Envoyer l'email
            mailSender.send(message);

            log.info("✅ Email de rejet de claim envoyé à {} pour le claim {} (raison: {})",
                    client.getEmail(), claim.getClaimId(), rejectionReason);

            // 🔥 AJOUT SMS
            String shortReason = rejectionReason != null && rejectionReason.length() > 50
                    ? rejectionReason.substring(0, 47) + "..."
                    : rejectionReason;
            String smsMessage = String.format("Réclamation %s rejetée. Raison: %s",
                    claim.getClaimId(), shortReason != null ? shortReason : "Non spécifiée");
            sendSmsIfAvailable(client, smsMessage);

        } catch (MessagingException e) {
            log.error("❌ Erreur lors de l'envoi de l'email de rejet de claim à {}: {}",
                    client.getEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur inattendue: {}", e.getMessage());
        }
    }


    /**
     * 📧 Envoyer un email de confirmation de paiement
     */
    @Async
    public void sendPaymentConfirmationEmail(Client client, InsuranceContract contract, Payment payment) {
        try {
            if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
                log.error("Email client manquant pour le contrat {}", contract.getContractId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(client.getEmail());
            helper.setSubject("✅ Confirmation de paiement - Contrat #" + contract.getContractId());

            // Préparer le contexte Thymeleaf
            Context context = new Context();
            context.setVariable("clientName", client.getFirstName() + " " + client.getLastName());
            context.setVariable("contractId", contract.getContractId());
            context.setVariable("paymentAmount", String.format("%.3f", payment.getAmount()));
            context.setVariable("paymentDate", new SimpleDateFormat("dd/MM/yyyy HH:mm").format(payment.getPaymentDate()));
            context.setVariable("paymentMethod", payment.getPaymentMethod() != null ?
                    payment.getPaymentMethod().toString() : "CARTE");
            context.setVariable("remainingAmount", String.format("%.3f", contract.getRemainingAmount()));
            context.setVariable("totalPaid", String.format("%.3f", contract.getTotalPaid()));
            context.setVariable("premium", String.format("%.3f", contract.getPremium()));

            context.setVariable("paymentReference", "PAY-" + payment.getPaymentId());

            context.setVariable("agentName", contract.getAgentAssurance() != null ?
                    contract.getAgentAssurance().getFirstName() + " " + contract.getAgentAssurance().getLastName() : "Votre agent");
            context.setVariable("agentEmail", contract.getAgentAssurance() != null ?
                    contract.getAgentAssurance().getEmail() : "");
            context.setVariable("currentDate", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

            String htmlContent = templateEngine.process("payment-confirmation", context);
            helper.setText(htmlContent, true);

            // Envoyer l'email
            mailSender.send(message);

            log.info("✅ Email de confirmation de paiement envoyé à {} pour le contrat {} (paiement: {} DT)",
                    client.getEmail(), contract.getContractId(), payment.getAmount());

            // 🔥 AJOUT SMS
            String smsMessage = String.format("Paiement de %.2f DT recu pour votre contrat. Merci! Reste: %.2f DT",
                    payment.getAmount(), contract.getRemainingAmount());
            sendSmsIfAvailable(client, smsMessage);

        } catch (MessagingException e) {
            log.error("❌ Erreur lors de l'envoi de l'email de confirmation de paiement à {}: {}",
                    client.getEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur inattendue: {}", e.getMessage());
        }
    }

    /**
     * 🎉 Envoyer un email de félicitations pour contrat complété (tous les paiements effectués)
     */
    @Async
    public void sendContractCompletedEmail(Client client, InsuranceContract contract) {
        try {
            if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
                log.error("Email client manquant pour le contrat {}", contract.getContractId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(client.getEmail());
            helper.setSubject("🎉 Félicitations ! Votre contrat d'assurance est entièrement payé");

            // Préparer le contexte Thymeleaf
            Context context = new Context();
            context.setVariable("clientName", client.getFirstName() + " " + client.getLastName());
            context.setVariable("contractId", contract.getContractId());
            context.setVariable("startDate", new SimpleDateFormat("dd/MM/yyyy").format(contract.getStartDate()));
            context.setVariable("endDate", new SimpleDateFormat("dd/MM/yyyy").format(contract.getEndDate()));
            context.setVariable("totalPaid", String.format("%.3f", contract.getTotalPaid()));
            context.setVariable("premium", String.format("%.3f", contract.getPremium()));
            context.setVariable("paymentFrequency", contract.getPaymentFrequency() != null ?
                    contract.getPaymentFrequency().toString() : "Mensuel");
            context.setVariable("agentName", contract.getAgentAssurance() != null ?
                    contract.getAgentAssurance().getFirstName() + " " + contract.getAgentAssurance().getLastName() : "Votre agent");
            context.setVariable("agentEmail", contract.getAgentAssurance() != null ?
                    contract.getAgentAssurance().getEmail() : "");
            context.setVariable("currentDate", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));

            // Calculer la durée du contrat
            long durationMonths = (contract.getEndDate().getTime() - contract.getStartDate().getTime()) / (1000L * 60 * 60 * 24 * 30);
            context.setVariable("durationMonths", durationMonths);

            String htmlContent = templateEngine.process("contract-completed", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ Email de contrat complété envoyé à {} pour le contrat {}",
                    client.getEmail(), contract.getContractId());

            // 🔥 AJOUT SMS
            String smsMessage = String.format("Félicitations! Contrat %s entièrement payé. Merci pour votre confiance!",
                    contract.getContractId());
            sendSmsIfAvailable(client, smsMessage);

        } catch (MessagingException e) {
            log.error("❌ Erreur lors de l'envoi de l'email de contrat complété à {}: {}",
                    client.getEmail(), e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur inattendue: {}", e.getMessage());
        }
    }
}