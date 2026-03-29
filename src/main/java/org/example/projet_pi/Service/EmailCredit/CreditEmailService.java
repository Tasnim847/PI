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
        message.setSubject("🔔 Payment Reminder – Due in 3 Days");

        String text = String.format(
                "Hello %s,\n\n" +
                        "This is a friendly reminder that your payment of %.2f TND for Loan No.%d " +
                        "is due in 3 days, on %s.\n\n" +
                        "Please make your payment before this date to avoid late fees.\n\n" +
                        "Best regards,\n" +
                        "Your Finance Team",
                clientName, amount, creditId, dueDate.toString()
        );
        message.setText(text);

        mailSender.send(message);
    }

    public void sendLatePaymentNotification(String toEmail, String clientName,
                                            Double amount, LocalDate dueDate, Long creditId) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("hamoudameriem317@gmail.com");
        message.setTo(toEmail);
        message.setSubject("⚠️ Late Payment – Loan No." + creditId);

        String text = String.format(
                "Hello %s,\n\n" +
                        "We have noticed that your payment of %.2f TND for Loan No.%d " +
                        "was due on %s and has not yet been received.\n\n" +
                        "Please settle your account as soon as possible " +
                        "to avoid additional penalties.\n\n" +
                        "Best regards,\n" +
                        "Your Finance Team",
                clientName, amount, creditId, dueDate.toString()
        );

        message.setText(text);

        mailSender.send(message);
    }

    public void sendAutoRejectionNotification(String toEmail, String clientName,
                                              Double amount, Double averageLatePercentage) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("hamoudameriem317@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject("❌ Loan Request Rejected - Late Payment History");

            String text = String.format(
                    "Hello %s,\n\n" +
                            "We regret to inform you that your loan request of %.2f TND " +
                            "has been automatically rejected.\n\n" +
                            "🔍 Reason: High late payment history (%.2f%%)\n" +
                            "Our policy requires a late payment history below 40%% for approval.\n\n" +
                            "We invite you to regularize your payments and submit a new request " +
                            "in the coming months.\n\n" +
                            "Best regards,\n" +
                            "Your Finance Team",
                    clientName, amount, averageLatePercentage
            );

            helper.setText(text);
            mailSender.send(message);

        } catch (MessagingException e) {
            // Silencieux
        }
    }

    public void sendEmailWithPdf(String toEmail, String clientName,
                                 Long creditId, byte[] pdfContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("lotfimakhlouf2000@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject("📄 Amortization Schedule – Loan No." + creditId);

            String text = String.format(
                    "Hello %s,\n\n" +
                            "Please find attached the amortization schedule for your loan No. %d.\n\n" +
                            "This document details the timetable of your payments.\n\n" +
                            "Best regards,\n" +
                            "Your finance team",
                    clientName, creditId
            );

            helper.setText(text);

            ByteArrayResource attachment = new ByteArrayResource(pdfContent);
            helper.addAttachment("loan_amortization_" + creditId + ".pdf", attachment);

            mailSender.send(message);

        } catch (MessagingException e) {
            // Silencieux
        }
    }

    // ✅ NOUVELLE MÉTHODE: Notification à l'admin pour une demande de crédit
    public void sendCreditRequestNotification(String toAdminEmail, String adminName,
                                              String clientName, Double amount,
                                              int duration, Long creditId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("hamoudameriem317@gmail.com");
            helper.setTo(toAdminEmail);
            helper.setSubject("🔔 Nouvelle demande de crédit à examiner");

            String text = String.format(
                    "Bonjour %s,\n\n" +
                            "Un nouveau client a fait une demande de crédit.\n\n" +
                            "📋 Détails de la demande :\n" +
                            "   • Client : %s\n" +
                            "   • Montant : %.2f TND\n" +
                            "   • Durée : %d mois\n" +
                            "   • Référence : CRD-%d\n\n" +
                            "🔗 Pour traiter cette demande, veuillez vous connecter à l'application.\n\n" +
                            "Cordialement,\n" +
                            "Votre système de gestion de crédit",
                    adminName, clientName, amount, duration, creditId
            );

            helper.setText(text);
            mailSender.send(message);

            System.out.println("✅ Notification de demande de crédit envoyée à l'admin: " + toAdminEmail);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur envoi notification admin: " + e.getMessage());
        }
    }
}