package org.example.projet_pi.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService2 {

    private final JavaMailSender mailSender;

    public void sendWelcomeEmail(String toEmail, String firstName){

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("Bienvenue sur notre plateforme");
        message.setText("Bienvenue  " + firstName +
                ",\n\nVotre compte a été créé avec succès.\nBienvenue !");

        mailSender.send(message);
    }
}