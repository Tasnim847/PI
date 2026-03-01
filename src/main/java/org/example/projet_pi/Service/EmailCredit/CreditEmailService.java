package org.example.projet_pi.Service.EmailCredit;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
@Service

public class CreditEmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPaymentReminder(String toEmail, String clientName,
                                    Double amount, LocalDate dueDate, Long creditId) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("hamoudameriem317@gmail.com");
        message.setTo(toEmail);
        message.setSubject("🔔 Rappel de paiement - Échéance dans 3 jours");

        String text = String.format(
                "Bonjour %s,\n\n" +
                        "Ceci est un rappel amical que votre paiement de %.2f TND pour le crédit N°%d " +
                        "sera dû dans 3 jours, le %s.\n\n" +
                        "Veuillez effectuer votre paiement avant cette date pour éviter des frais de retard.\n\n" +
                        "Cordialement,\n" +
                        "Votre équipe financière",
                clientName, amount, creditId, dueDate.toString()
        );

        message.setText(text);

        mailSender.send(message);
    }

    public void sendLatePaymentNotification(String toEmail, String clientName,
                                            Double amount, LocalDate dueDate, Long creditId) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("votre-email@gmail.com");
        message.setTo(toEmail);
        message.setSubject("⚠️ Paiement en retard - Crédit N°" + creditId);

        String text = String.format(
                "Bonjour %s,\n\n" +
                        "Nous constatons que votre paiement de %.2f TND pour le crédit N°%d " +
                        "était dû le %s et n'a pas encore été reçu.\n\n" +
                        "Veuillez régulariser votre situation dans les plus brefs délais " +
                        "pour éviter des pénalités supplémentaires.\n\n" +
                        "Cordialement,\n" +
                        "Votre équipe financière",
                clientName, amount, creditId, dueDate.toString()
        );

        message.setText(text);

        mailSender.send(message);
    }
    public void sendEmailWithPdf(String toEmail, String clientName,
                                 Long creditId, byte[] pdfContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("hamoudameriem317@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject("📄 Tableau d'amortissement - Crédit N°" + creditId);

            String text = String.format(
                    "Bonjour %s,\n\n" +
                            "Veuillez trouver ci-joint le tableau d'amortissement pour votre crédit N°%d.\n\n" +
                            "Ce document détaille l'échéancier de vos paiements.\n\n" +
                            "Cordialement,\n" +
                            "Votre équipe financière",
                    clientName, creditId
            );

            helper.setText(text);

            // ✅ Ajouter la pièce jointe PDF
            ByteArrayResource attachment = new ByteArrayResource(pdfContent);
            helper.addAttachment("amortissement_credit_" + creditId + ".pdf", attachment);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email avec PDF: " + e.getMessage());
        }
    }
}
