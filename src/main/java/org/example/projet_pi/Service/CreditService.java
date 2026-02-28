package org.example.projet_pi.Service;

import jakarta.transaction.Transactional;
import org.example.projet_pi.Dto.CreditHistoryDTO;
import org.example.projet_pi.Repository.CreditRepository;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Credit;
import org.example.projet_pi.entity.CreditStatus;
import org.example.projet_pi.entity.RepaymentStatus;
import org.springframework.stereotype.Service;
import org.example.projet_pi.Dto.CreditHistoryWithAverageDTO ;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CreditService implements ICreditService {

    private final CreditRepository creditRepository;

    public CreditService(CreditRepository creditRepository) {
        this.creditRepository = creditRepository;
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
    public Credit addCredit(Credit credit) {

        if (credit.getAmount() <= 0) {
            throw new IllegalArgumentException("Montant invalide");
        }

        if (credit.getDurationInMonths() <= 0) {
            throw new IllegalArgumentException("Durée invalide");
        }

        // 🔒 Toujours PENDING à la création
        credit.setStatus(CreditStatus.PENDING);

        // ❌ On ne touche pas aux champs calculés ici
        // interestRate, monthlyPayment, startDate, endDate restent null/0
        // dueDate peut être fourni par le client si nécessaire

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

        // ✅ Calcul du paiement mensuel
        double amount = credit.getAmount();
        int duration = credit.getDurationInMonths();

        double totalToPay = amount + (amount * interestRate / 100.0);
        double monthlyPayment = totalToPay / duration;

        credit.setMonthlyPayment((float) monthlyPayment);


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