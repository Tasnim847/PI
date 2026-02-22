package org.example.projet_pi.Service;

import jakarta.transaction.Transactional;
import org.example.projet_pi.Repository.CreditRepository;
import org.example.projet_pi.entity.Credit;
import org.example.projet_pi.entity.CreditStatus;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CreditService implements ICreditService {

    private final CreditRepository creditRepository;

    public CreditService(CreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }

    // ===============================
    // 1️⃣ CREATE CREDIT (Client)
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

        // ❌ Client ne peut pas définir ces champs
        credit.setInterestRate(0);
        credit.setMonthlyPayment(0);
        credit.setStartDate(null);
        credit.setEndDate(null);

        return creditRepository.save(credit);
    }

    // ===============================
    // 2️⃣ APPROVE CREDIT (AgentFinance)
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

        // ✅ 1️⃣ définir le taux
        credit.setInterestRate(interestRate);

        // ✅ 2️⃣ calcul bancaire
        double amount = credit.getAmount();
        int duration = credit.getDurationInMonths();

        double totalToPay = amount + (amount * interestRate / 100.0);
        double monthlyPayment = totalToPay / duration;

        credit.setMonthlyPayment(monthlyPayment);

        // ✅ 3️⃣ dates
        Date now = new Date();
        credit.setStartDate(now);

        Date endDate = new Date(
                now.getTime() + (long) duration * 30 * 24 * 60 * 60 * 1000
        );
        credit.setEndDate(endDate);

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
}