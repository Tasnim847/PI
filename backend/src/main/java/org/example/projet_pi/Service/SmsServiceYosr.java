package org.example.projet_pi.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsServiceYosr {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromNumber;

    public void sendSms(String to, String message) {
        if (to == null || to.isEmpty() || to.isBlank()) {
            System.out.println("⚠️ SMS ignoré - numéro absent");
            return;
        }

        if (!to.matches("^\\+216\\d{8}$")) {
            System.out.println("⚠️ SMS ignoré - format invalide: " + to);
            return;
        }

        if (message == null || message.isEmpty()) {
            System.out.println("⚠️ SMS ignoré - message vide");
            return;
        }

        try {
            Twilio.init(accountSid, authToken);
            Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromNumber),
                    message
            ).create();
            System.out.println("✅ SMS envoyé à: " + to);
        } catch (Exception e) {
            System.err.println("⚠️ Erreur SMS pour " + to + ": " + e.getMessage());
        }
    }
}