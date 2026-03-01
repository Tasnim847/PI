package org.example.projet_pi.Service.EmailCredit;

import org.example.projet_pi.Repository.CreditRepository;
import org.example.projet_pi.entity.Credit;
import org.example.projet_pi.entity.CreditStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CreditNotificationScheduler {

    private final CreditRepository creditRepository;
    private final CreditEmailService creditEmailService;

    public CreditNotificationScheduler(CreditRepository creditRepository,
                                       CreditEmailService creditEmailService) {
        this.creditRepository = creditRepository;
        this.creditEmailService = creditEmailService;
    }

    /**
     * Vérifie tous les jours à 8h du matin les crédits dont l'échéance est dans 3 jours
     */
    @Scheduled(cron = "0 0 8 * * ?") // Tous les jours à 08:00
    public void sendUpcomingDueDateReminders() {
        System.out.println("🔍 Vérification des échéances dans 3 jours...");

        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.plusDays(3); // Dans 3 jours

        // Chercher les crédits avec dueDate = targetDate et statut approprié
        List<Credit> creditsDueSoon = creditRepository.findByDueDateAndStatusIn(
                targetDate,
                List.of(CreditStatus.APPROVED, CreditStatus.IN_REPAYMENT)
        );

        System.out.println("📊 " + creditsDueSoon.size() + " crédits trouvés avec échéance dans 3 jours");

        for (Credit credit : creditsDueSoon) {
            try {
                if (credit.getClient() != null && credit.getClient().getEmail() != null) {
                    String clientName = credit.getClient().getFirstName() + " " +
                            credit.getClient().getLastName();

                    creditEmailService.sendPaymentReminder(
                            credit.getClient().getEmail(),
                            clientName,
                            credit.getMonthlyPayment(),
                            credit.getDueDate(),
                            credit.getCreditId()
                    );

                    System.out.println("✅ Rappel envoyé à " + credit.getClient().getEmail() +
                            " pour le crédit N°" + credit.getCreditId());
                } else {
                    System.out.println("⚠️ Crédit N°" + credit.getCreditId() +
                            " n'a pas de client ou d'email associé");
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de l'envoi pour le crédit N°" +
                        credit.getCreditId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Vérifie les crédits en retard (optionnel)
     */
    @Scheduled(cron = "0 0 9 * * ?") // Tous les jours à 09:00
    public void checkLatePayments() {
        LocalDate today = LocalDate.now();

        List<Credit> lateCredits = creditRepository.findByDueDateBeforeAndStatusIn(
                today,
                List.of(CreditStatus.APPROVED, CreditStatus.IN_REPAYMENT)
        );

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
    }

    /**
     * Endpoint pour tester manuellement (à ajouter dans votre controller)
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
        }
    }
}