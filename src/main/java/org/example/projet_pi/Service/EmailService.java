package org.example.projet_pi.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.InsuranceContract;
import org.example.projet_pi.entity.Payment;
import org.example.projet_pi.entity.PaymentReminder;
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

    // Dans EmailService.java, modifiez la méthode getSubjectByDaysBefore :

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

                // Petit délai pour ne pas surcharger le serveur SMTP
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Erreur pour {}: {}", client.getEmail(), e.getMessage());
            }
        }
    }
}