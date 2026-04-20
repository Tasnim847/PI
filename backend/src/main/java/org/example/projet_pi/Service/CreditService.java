package org.example.projet_pi.Service;

import jakarta.transaction.Transactional;
import org.example.projet_pi.Dto.CreditHistoryDTO;
import org.example.projet_pi.Dto.CreditHistoryWithAverageDTO;
import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.Repository.CreditRepository;
import org.example.projet_pi.Service.EmailCredit.CreditEmailService;
import org.example.projet_pi.entity.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CreditService implements ICreditService {

    private final CreditRepository creditRepository;
    private final ClientRepository clientRepository;
    private final CreditEmailService creditEmailService;  // ✅ AJOUT
    private final AccountRepository accountRepository;

    // ✅ MODIFIER LE CONSTRUCTEUR
    public CreditService(CreditRepository creditRepository,
                         ClientRepository clientRepository,
                         CreditEmailService creditEmailService,
                         AccountRepository accountRepository) {  // ✅ AJOUT
        this.creditRepository = creditRepository;
        this.clientRepository = clientRepository;
        this.creditEmailService = creditEmailService;
        this.accountRepository = accountRepository;  // ✅ INITIALISER
    }

    public List<CreditHistoryDTO> getClosedCreditsWithLateRepaymentPercentage(Client client) {
        List<Credit> closedCredits = creditRepository.findByClientAndStatus(client, CreditStatus.CLOSED);

        return closedCredits.stream().map(credit -> {
            long totalRepayments = credit.getRepayments().size();
            long lateRepayments = credit.getRepayments().stream()
                    .filter(r -> r.getStatus() == RepaymentStatus.LATE)
                    .count();

            double latePercentage = totalRepayments > 0 ?
                    ((double) lateRepayments / totalRepayments) * 100 : 0;

            return new CreditHistoryDTO(credit, latePercentage);
        }).collect(Collectors.toList());
    }

    public CreditHistoryWithAverageDTO getClosedCreditsWithAverage(Client client) {
        List<CreditHistoryDTO> closedCredits = getClosedCreditsWithLateRepaymentPercentage(client);

        double average = 0;
        if (!closedCredits.isEmpty()) {
            average = closedCredits.stream()
                    .mapToDouble(CreditHistoryDTO::getLateRepaymentPercentage)
                    .average()
                    .orElse(0);
        }

        return new CreditHistoryWithAverageDTO(closedCredits, average);
    }

    // ===============================
    // 1 CREATE CREDIT (Client)
    // ===============================
    @Override
    @Transactional
    public Credit addCredit(Credit credit, Admin admin) {

        if (credit.getAmount() <= 0) {
            throw new IllegalArgumentException("Montant invalide");
        }

        if (credit.getDurationInMonths() <= 0) {
            throw new IllegalArgumentException("Durée invalide");
        }

        // Vérifier que le client est spécifié
        if (credit.getClient() == null || credit.getClient().getId() == null) {
            throw new IllegalArgumentException("Le client doit être spécifié");
        }

        // 🔒 Récupérer le client complet depuis la base
        Client client = clientRepository.findById(credit.getClient().getId())
                .orElseThrow(() -> new IllegalArgumentException("Client non trouvé avec l'id: " + credit.getClient().getId()));

        // ✅ NOUVEAU: VÉRIFICATION DE L'EXISTENCE D'UN COMPTE
        List<Account> clientAccounts = accountRepository.findByClientId(client.getId());

        if (clientAccounts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Le client doit avoir un compte bancaire pour obtenir un crédit. " +
                            "Veuillez créer un compte pour le client avant de procéder."
            );
        }

        // ✅ Vérification optionnelle: compte actif
        boolean hasActiveAccount = clientAccounts.stream()
                .anyMatch(account -> "ACTIVE".equalsIgnoreCase(account.getStatus()));

        if (!hasActiveAccount) {
            throw new IllegalArgumentException(
                    "Le client doit avoir un compte ACTIF pour obtenir un crédit."
            );
        }

        // ✅ VÉRIFICATION DE L'HISTORIQUE DE RETARDS
        List<Credit> closedCredits = creditRepository.findByClientAndStatus(client, CreditStatus.CLOSED);

        if (!closedCredits.isEmpty()) {
            double totalLatePercentage = 0;
            int creditCount = 0;

            for (Credit closedCredit : closedCredits) {
                long totalRepayments = closedCredit.getRepayments().size();
                long lateRepayments = closedCredit.getRepayments().stream()
                        .filter(r -> r.getStatus() == RepaymentStatus.LATE)
                        .count();

                double latePercentage = totalRepayments > 0 ?
                        ((double) lateRepayments / totalRepayments) * 100 : 0;

                totalLatePercentage += latePercentage;
                creditCount++;
            }

            double averageLatePercentage = totalLatePercentage / creditCount;

            // 🔴 SI LA MOYENNE DÉPASSE 40% → REJET AUTOMATIQUE
            if (averageLatePercentage > 40) {
                // Récupérer l'agent financier du client
                AgentFinance agentFinance = client.getAgentFinance();

                // Assigner toutes les relations
                credit.setClient(client);
                credit.setAdmin(admin);
                credit.setAgentFinance(agentFinance);
                credit.setStatus(CreditStatus.REJECTED); // ✅ REJETÉ AUTOMATIQUEMENT

                // Envoyer l'email de notification
                String clientName = client.getFirstName() + " " + client.getLastName();
                creditEmailService.sendAutoRejectionNotification(
                        client.getEmail(),
                        clientName,
                        credit.getAmount(),
                        averageLatePercentage
                );

                System.out.println("❌ Crédit automatiquement rejeté pour le client " + clientName +
                        " - Moyenne de retards: " + averageLatePercentage + "%");

                return creditRepository.save(credit);
            }
        }

        // ✅ Si l'historique est bon, continuer normalement
        AgentFinance agentFinance = client.getAgentFinance();
        if (agentFinance == null) {
            throw new IllegalArgumentException("Le client n'a pas d'agent financier assigné");
        }

        // ✅ Assigner toutes les relations
        credit.setClient(client);
        credit.setAdmin(admin);
        credit.setAgentFinance(agentFinance);

        // 🔒 Toujours PENDING à la création
        credit.setStatus(CreditStatus.PENDING);

        System.out.println("✅ Crédit créé en statut PENDING pour le client " +
                client.getFirstName() + " " + client.getLastName());

        return creditRepository.save(credit);
    }

    // ===============================
    //  APPROVE CREDIT (AgentFinance)
    // ===============================
    @Transactional
    public Credit approveCredit(Long creditId, double interestRate) {

        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit introuvable"));

        if (credit.getStatus() != CreditStatus.PENDING) {
            throw new IllegalStateException("Seul un crédit PENDING peut être approuvé");
        }

        if (interestRate <= 0) {
            throw new IllegalArgumentException("Taux invalide");
        }

        // ✅ Définir le taux
        credit.setInterestRate(interestRate);

        // Montant et durée
        double amount = credit.getAmount();
        int duration = credit.getDurationInMonths();

        // taux mensuel en décimal
        double monthlyRate = (interestRate / 100.0) / 12.0;

        // Calcul mensualité constante
        float monthlyPayment;

        if (monthlyRate == 0) {
            monthlyPayment = (float) (amount / duration);
        } else {
            monthlyPayment = (float) ((amount * monthlyRate) /
                    (1 - Math.pow(1 + monthlyRate, -duration)));
        }

        credit.setMonthlyPayment(monthlyPayment);
        float TotalPayement = monthlyPayment * duration;

        // ✅ Dates
        Date now = new Date();
        credit.setStartDate(now);

        Date endDate = new Date(
                now.getTime() + (long) duration * 30 * 24 * 60 * 60 * 1000
        );
        credit.setEndDate(endDate);

        // 🔒 Status
        credit.setStatus(CreditStatus.APPROVED);

        return creditRepository.save(credit);
    }

    // ===============================
    // 3️⃣ REJECT CREDIT (AgentFinance)
    // ===============================
    @Transactional
    public Credit rejectCredit(Long creditId) {

        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit introuvable"));

        if (credit.getStatus() != CreditStatus.PENDING) {
            throw new IllegalStateException("Seul un crédit PENDING peut être rejeté");
        }

        credit.setStatus(CreditStatus.REJECTED);

        return creditRepository.save(credit);
    }

    // ===============================
    // CRUD
    // ===============================
    @Override
    public Credit updateCredit(Credit credit) {
        return creditRepository.save(credit);
    }

    @Override
    public void deleteCredit(Long id) {
        creditRepository.deleteById(id);
    }

    @Override
    public Credit getCreditById(Long id) {
        return creditRepository.findById(id).orElse(null);
    }

    @Override
    public List<Credit> getAllCredits() {
        return creditRepository.findAllWithClient();  // ← Remplace findAll()
    }


    @Override
    public List<Credit> getCreditsByClientEmail(String email) {
        return creditRepository.findByClient_Email(email);
    }
}