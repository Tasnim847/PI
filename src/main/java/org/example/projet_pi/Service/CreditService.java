package org.example.projet_pi.Service;

import jakarta.transaction.Transactional;
import org.example.projet_pi.Dto.CreditHistoryDTO;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.Repository.CreditRepository;
import org.example.projet_pi.entity.*;
import org.springframework.stereotype.Service;
import org.example.projet_pi.Dto.CreditHistoryWithAverageDTO ;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CreditService implements ICreditService {

    private final CreditRepository creditRepository;
    private final ClientRepository clientRepository;

    public CreditService(CreditRepository creditRepository , ClientRepository clientRepository) {
        this.creditRepository = creditRepository;
        this.clientRepository = clientRepository;
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
    public Credit addCredit(Credit credit, Admin admin) {  // ✅ Ajout du paramètre admin

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

        // ✅ Récupérer l'agent financier du client
        AgentFinance agentFinance = client.getAgentFinance();
        if (agentFinance == null) {
            // Option 1: Lever une exception
            throw new IllegalArgumentException("Le client n'a pas d'agent financier assigné");

            // Option 2: Assigner un agent par défaut (si vous préférez)
            // agentFinance = agentFinanceRepository.findDefaultAgent();
        }

        // ✅ Assigner toutes les relations
        credit.setClient(client);                 // Le client
        credit.setAdmin(admin);                    // L'admin qui crée le crédit
        credit.setAgentFinance(agentFinance);      // L'agent financier du client

        // 🔒 Toujours PENDING à la création
        credit.setStatus(CreditStatus.PENDING);

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
        return creditRepository.findAll();
    }
    @Override
    public List<Credit> getCreditsByClientEmail(String email) {
        return creditRepository.findByClient_Email(email);
    }
}