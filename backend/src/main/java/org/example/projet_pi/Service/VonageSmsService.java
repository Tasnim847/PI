package org.example.projet_pi.Service;

import com.vonage.client.VonageClient;
import com.vonage.client.sms.SmsSubmissionResponse;
import com.vonage.client.sms.SmsSubmissionResponseMessage;
import com.vonage.client.sms.messages.TextMessage;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VonageSmsService {

    private VonageClient vonageClient;

    @Value("${vonage.api.key}")
    private String apiKey;

    @Value("${vonage.api.secret}")
    private String apiSecret;

    @Value("${vonage.from.number}")
    private String fromNumber;

    @Value("${vonage.sms.enabled:true}")
    private boolean smsEnabled;

    @PostConstruct
    public void init() {
        if (smsEnabled && apiKey != null && apiSecret != null && !apiKey.isEmpty() && !apiSecret.isEmpty()) {
            try {
                this.vonageClient = VonageClient.builder()
                        .apiKey(apiKey)
                        .apiSecret(apiSecret)
                        .build();
                log.info("✅ Service SMS Vonage initialisé avec succès");
                log.info("📱 Numéro d'envoi: {}", fromNumber);
            } catch (Exception e) {
                log.error("❌ Erreur initialisation Vonage: {}", e.getMessage());
                this.smsEnabled = false;
            }
        } else {
            log.warn("⚠️ Service SMS Vonage désactivé (mode développement)");
        }
    }

    /**
     * Envoyer un SMS à un numéro de téléphone
     * @param phoneNumber Numéro du destinataire (format international: +216XXXXXXXX)
     * @param message Contenu du message (max 160 caractères)
     */
    public void sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.info("📱 SMS désactivé - Message non envoyé à {}: {}", phoneNumber, message);
            return;
        }

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.error("❌ Numéro de téléphone manquant");
            return;
        }

        if (message == null || message.trim().isEmpty()) {
            log.error("❌ Message SMS vide");
            return;
        }

        // Limiter la longueur du message (160 caractères pour SMS standard)
        String finalMessage = message;
        if (finalMessage.length() > 160) {
            finalMessage = finalMessage.substring(0, 157) + "...";
            log.warn("⚠️ Message tronqué à 160 caractères");
        }

        try {
            // Formater le numéro au format international
            String formattedNumber = formatPhoneNumber(phoneNumber);

            // Créer le message SMS
            TextMessage sms = new TextMessage(fromNumber, formattedNumber, finalMessage);

            // Envoyer le SMS
            SmsSubmissionResponse response = vonageClient.getSmsClient().submitMessage(sms);

            // 🔥 VERSION SIMPLIFIÉE - Vérifier juste si l'envoi a réussi
            if (response != null && response.getMessages() != null && !response.getMessages().isEmpty()) {
                log.info("✅ SMS envoyé avec succès à {}", formattedNumber);
            } else {
                log.error("❌ Erreur envoi SMS à {}", formattedNumber);
            }

        } catch (Exception e) {
            log.error("❌ Exception lors de l'envoi SMS à {}: {}", phoneNumber, e.getMessage());
        }
    }

    /**
     * Envoyer un SMS avec un template
     * @param phoneNumber Numéro du destinataire
     * @param template Template du message (format String.format)
     * @param args Arguments du template
     */
    public void sendTemplateSms(String phoneNumber, String template, Object... args) {
        String message = String.format(template, args);
        sendSms(phoneNumber, message);
    }

    /**
     * Formater le numéro de téléphone au format international
     * Formats acceptés:
     * - 12345678 -> +21612345678 (Tunisie)
     * - 012345678 -> +21612345678
     * - +21612345678 -> +21612345678
     * - 0021612345678 -> +21612345678
     */
    private String formatPhoneNumber(String phoneNumber) {
        // Supprimer tous les espaces et tirets
        String cleaned = phoneNumber.replaceAll("[\\s\\-]", "");

        // Si déjà au format international avec +
        if (cleaned.startsWith("+")) {
            return cleaned;
        }

        // Si commence par 00 (format international sans +)
        if (cleaned.startsWith("00")) {
            return "+" + cleaned.substring(2);
        }

        // Si commence par 0 (numéro local tunisien)
        if (cleaned.startsWith("0")) {
            return "+216" + cleaned.substring(1);
        }

        // Si numéro sans indicatif (supposé tunisien)
        if (cleaned.length() == 8) {
            return "+216" + cleaned;
        }

        // Par défaut, ajouter +216
        return "+216" + cleaned;
    }

    /**
     * Vérifier si le service SMS est disponible
     */
    public boolean isAvailable() {
        return smsEnabled && vonageClient != null;
    }

    /**
     * Envoyer un SMS de test
     */
    public void sendTestSms(String phoneNumber) {
        String testMessage = "Test: Votre service SMS Vonage fonctionne correctement. " +
                "Date: " + java.time.LocalDateTime.now();
        sendSms(phoneNumber, testMessage);
    }
}