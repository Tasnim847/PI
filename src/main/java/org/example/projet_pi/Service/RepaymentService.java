package org.example.projet_pi.Service;

import jakarta.transaction.Transactional;
import org.example.projet_pi.Repository.CreditRepository;
import org.example.projet_pi.Repository.RepaymentRepository;
import org.example.projet_pi.entity.Credit;
import org.example.projet_pi.entity.CreditStatus;
import org.example.projet_pi.entity.Repayment;
import org.example.projet_pi.entity.RepaymentStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class RepaymentService implements IRepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final CreditRepository creditRepository;

    public RepaymentService(RepaymentRepository repaymentRepository,
                            CreditRepository creditRepository) {
        this.repaymentRepository = repaymentRepository;
        this.creditRepository = creditRepository;
    }

    // ===== CRUD =====
    @Override
    public Repayment addRepayment(Repayment repayment) {
        return repaymentRepository.save(repayment);
    }

    @Override
    public Repayment updateRepayment(Repayment repayment) {
        return repaymentRepository.save(repayment);
    }

    @Override
    public void deleteRepayment(Long id) {
        repaymentRepository.deleteById(id);
    }

    @Override
    public Repayment getRepaymentById(Long id) {
        return repaymentRepository.findById(id).orElse(null);
    }

    @Override
    public List<Repayment> getAllRepayments() {
        return repaymentRepository.findAll();
    }

    // ===== METIER : payer un crédit =====
    @Override
    @Transactional
    public Repayment payCredit(Long creditId, Repayment repayment) {

        // 1) Validation basique
        if (repayment == null) {
            throw new IllegalArgumentException("Repayment est null");
        }
        if (repayment.getAmount() == null || repayment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant doit être > 0");
        }
        if (repayment.getPaymentMethod() == null) {
            throw new IllegalArgumentException("PaymentMethod obligatoire");
        }

        // 2) Charger le crédit
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit introuvable"));

        CreditStatus st = credit.getStatus();

        if (st == CreditStatus.CLOSED) {
            throw new IllegalStateException("Credit déjà fermé");
        }
        if (st == CreditStatus.REJECTED) {
            throw new IllegalStateException("Credit rejeté, paiement interdit");
        }
        if (st == CreditStatus.PENDING) {
            throw new IllegalStateException("Credit en attente, paiement interdit");
        }

        // 3) Date auto
        if (repayment.getPaymentDate() == null) {
            repayment.setPaymentDate(LocalDate.now());
        }

        // 4) Référence auto
        if (repayment.getReference() == null || repayment.getReference().isBlank()) {
            repayment.setReference("PAY-" + creditId + "-" + UUID.randomUUID().toString().substring(0, 8));
        }

        // 5) Total payé (PAID)
        BigDecimal totalPaid = repaymentRepository.sumPaidSuccess(creditId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        totalPaid = totalPaid.setScale(2, RoundingMode.HALF_UP);

        // 6) Calcul du montant total à payer
        BigDecimal monthly = BigDecimal.valueOf(credit.getMonthlyPayment()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPayable = monthly.multiply(BigDecimal.valueOf(credit.getDurationInMonths()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = totalPayable.subtract(totalPaid).setScale(2, RoundingMode.HALF_UP);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            credit.setStatus(CreditStatus.CLOSED);
            creditRepository.save(credit);
            throw new IllegalStateException("Credit déjà payé");
        }

        // 7) Vérification stricte du paiement
        BigDecimal amountToPay = repayment.getAmount().setScale(2, RoundingMode.HALF_UP);

        if (amountToPay.compareTo(monthly) < 0 || amountToPay.compareTo(remaining) > 0) {
            // Paiement trop faible ou trop élevé → FAILED
            repayment.setStatus(RepaymentStatus.FAILED);
            repayment.setCredit(credit);
            return repaymentRepository.save(repayment);
        }

        // 8) Paiement valide → PAID
        repayment.setStatus(RepaymentStatus.PAID);
        repayment.setCredit(credit);
        Repayment saved = repaymentRepository.save(repayment);

        // 9) Mise à jour du statut du crédit
        if (credit.getStatus() == CreditStatus.APPROVED) {
            credit.setStatus(CreditStatus.IN_REPAYMENT);
        }

        BigDecimal newTotalPaid = totalPaid.add(amountToPay).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newRemaining = totalPayable.subtract(newTotalPaid).setScale(2, RoundingMode.HALF_UP);

        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            credit.setStatus(CreditStatus.CLOSED);
        }

        creditRepository.save(credit);

        return saved;
    }
    /**
     * Retourne le montant restant à payer pour un crédit
     */
    public BigDecimal getRemainingAmount(Long creditId) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit introuvable"));

        BigDecimal monthly = BigDecimal.valueOf(credit.getMonthlyPayment()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPayable = monthly.multiply(BigDecimal.valueOf(credit.getDurationInMonths())).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPaid = repaymentRepository.sumPaidSuccess(creditId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        totalPaid = totalPaid.setScale(2, RoundingMode.HALF_UP);

        return totalPayable.subtract(totalPaid).setScale(2, RoundingMode.HALF_UP);
    }
}