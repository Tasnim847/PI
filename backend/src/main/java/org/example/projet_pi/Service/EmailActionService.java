package org.example.projet_pi.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailActionService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendTransactionEmail(
            String toEmail,
            String type,
            double amount,
            double newBalance) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Confirmation de transaction - " + type);
        message.setText(
                "Bonjour,\n\n" +
                        "Votre transaction a été effectuée avec succès.\n\n" +
                        "Type          : " + type + "\n" +
                        "Montant       : " + amount + " TND\n" +
                        "Nouveau solde : " + newBalance + " TND\n\n" +
                        "Merci de votre confiance.\n" +
                        "Votre Banque"
        );

        mailSender.send(message);
    }
}