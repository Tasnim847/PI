package org.example.projet_pi.Service.EmailCredit;

import org.example.projet_pi.Repository.CreditRepository;
import org.example.projet_pi.entity.Credit;
import org.example.projet_pi.entity.CreditStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class CreditNotificationScheduler {

    private final CreditRepository creditRepository;
    private final CreditEmailService creditEmailService;

    public CreditNotificationScheduler(CreditRepository creditRepository,
                                       CreditEmailService creditEmailService) {
        this.creditRepository = creditRepository;
        this.creditEmailService = creditEmailService;
    }

    /**
     * ✅ POUR TEST: S'exécute toutes les minutes
     * ⚠️ À CHANGER en "0 0 8 * * ?" après test
     */
    @Scheduled(cron = "0 0 8 * * ?")  // ✅ Toutes les minutes (pour tester)
    public void sendUpcomingDueDateReminders() {
        System.out.println("========================================");
        System.out.println("🔍 Vérification des échéances...");
        System.out.println("⏰ Heure: " + java.time.LocalTime.now());

        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(3); // Dans 3 jours

        System.out.println("📅 Aujourd'hui: " + today);
        System.out.println("🎯 Recherche des crédits avec échéance le: " + targetDate);

        // Chercher les crédits avec dueDate = targetDate et statut approprié
        List<Credit> creditsDueSoon = creditRepository.findByDueDateAndStatusIn(
                targetDate,
                List.of(CreditStatus.APPROVED, CreditStatus.IN_REPAYMENT)
        );

        System.out.println("📊 " + creditsDueSoon.size() + " crédits trouvés avec échéance dans 3 jours");

        if (creditsDueSoon.isEmpty()) {
            System.out.println("ℹ️ Aucun crédit trouvé pour le " + targetDate);
        }

        for (Credit credit : creditsDueSoon) {
            try {
                if (credit.getClient() != null && credit.getClient().getEmail() != null) {
                    String clientName = credit.getClient().getFirstName() + " " +
                            credit.getClient().getLastName();

                    System.out.println("📧 Envoi d'email à: " + credit.getClient().getEmail());
                    System.out.println("   - Crédit N°: " + credit.getCreditId());
                    System.out.println("   - Montant: " + credit.getMonthlyPayment() + " TND");
                    System.out.println("   - Échéance: " + credit.getDueDate());

                    creditEmailService.sendPaymentReminder(
                            credit.getClient().getEmail(),
                            clientName,
                            credit.getMonthlyPayment(),
                            credit.getDueDate(),
                            credit.getCreditId()
                    );

                    System.out.println("✅ Rappel envoyé avec succès à " + credit.getClient().getEmail());
                } else {
                    System.out.println("⚠️ Crédit N°" + credit.getCreditId() +
                            " n'a pas de client ou d'email associé");
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de l'envoi pour le crédit N°" +
                        credit.getCreditId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("========================================");
    }

    /**
     * Vérifie les crédits en retard (tous les jours à 09:00)
     */
    @Scheduled(cron = "0 0 9 * * ?") // Tous les jours à 09:00
    public void checkLatePayments() {
        System.out.println("========================================");
        System.out.println("🔍 Vérification des paiements en retard...");

        LocalDate today = LocalDate.now();

        List<Credit> lateCredits = creditRepository.findByDueDateBeforeAndStatusIn(
                today,
                List.of(CreditStatus.APPROVED, CreditStatus.IN_REPAYMENT)
        );

        System.out.println("📊 " + lateCredits.size() + " crédits en retard trouvés");

        for (Credit credit : lateCredits) {
            try {
                if (credit.getClient() != null && credit.getClient().getEmail() != null) {
                    String clientName = credit.getClient().getFirstName() + " " +
                            credit.getClient().getLastName();

                    creditEmailService.sendLatePaymentNotification(
                            credit.getClient().getEmail(),
                            clientName,
                            credit.getMonthlyPayment(),
                            credit.getDueDate(),
                            credit.getCreditId()
                    );

                    System.out.println("⚠️ Notification de retard envoyée pour le crédit N°" +
                            credit.getCreditId());
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur envoi retard: " + e.getMessage());
            }
        }
        System.out.println("========================================");
    }

    /**
     * Méthode pour tester manuellement (peut être appelée par un endpoint)
     */
    public void testReminderForCredit(Long creditId) {
        Credit credit = creditRepository.findById(creditId).orElse(null);
        if (credit != null && credit.getClient() != null) {
            creditEmailService.sendPaymentReminder(
                    credit.getClient().getEmail(),
                    credit.getClient().getFirstName() + " " + credit.getClient().getLastName(),
                    credit.getMonthlyPayment(),
                    credit.getDueDate(),
                    credit.getCreditId()
            );
            System.out.println("✅ Test réussi pour le crédit N°" + creditId);
        } else {
            System.out.println("❌ Crédit N°" + creditId + " non trouvé ou sans client");
        }
    }
}