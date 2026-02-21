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
    public Repayment payCredit(Long creditId, Repayment repayment, boolean allowPartialIfOverpay) {

        // 1) Validation
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

        // ✅ Vérifications métier sur statut
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

        // 5) Total payé (PAID) - SAFE si null
        BigDecimal totalPaid = repaymentRepository.sumPaidSuccess(creditId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        totalPaid = totalPaid.setScale(2, RoundingMode.HALF_UP);

        // 6) Total payable = monthlyPayment * duration
        BigDecimal monthly = BigDecimal.valueOf(credit.getMonthlyPayment()).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPayable = monthly
                .multiply(BigDecimal.valueOf(credit.getDurationInMonths()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal remaining = totalPayable.subtract(totalPaid).setScale(2, RoundingMode.HALF_UP);

        // Si déjà payé
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            credit.setStatus(CreditStatus.CLOSED);
            creditRepository.save(credit);
            throw new IllegalStateException("Credit déjà payé");
        }

        // 7) Gestion surpaiement
        BigDecimal acceptedAmount = repayment.getAmount().setScale(2, RoundingMode.HALF_UP);

        if (acceptedAmount.compareTo(remaining) > 0) {

            if (!allowPartialIfOverpay) {
                // surpaiement refusé => FAILED enregistré
                repayment.setStatus(RepaymentStatus.FAILED);
                repayment.setCredit(credit);
                return repaymentRepository.save(repayment);
            }

            // Accepter seulement le restant
            acceptedAmount = remaining;
            repayment.setAmount(acceptedAmount);
        }

        // 8) Save PAID
        repayment.setStatus(RepaymentStatus.PAID);
        repayment.setCredit(credit);

        Repayment saved = repaymentRepository.save(repayment);

        // ✅ Dès le 1er paiement, passer le crédit en IN_REPAYMENT
        if (credit.getStatus() == CreditStatus.APPROVED) {
            credit.setStatus(CreditStatus.IN_REPAYMENT);
        }

        // 9) Update credit status
        BigDecimal newTotalPaid = totalPaid.add(acceptedAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newRemaining = totalPayable.subtract(newTotalPaid).setScale(2, RoundingMode.HALF_UP);

        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            credit.setStatus(CreditStatus.CLOSED);
        }

        creditRepository.save(credit);

        return saved;
    }
}