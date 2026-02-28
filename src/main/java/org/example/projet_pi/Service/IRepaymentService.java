package org.example.projet_pi.Service;

import org.example.projet_pi.entity.Repayment;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public interface IRepaymentService {
    Repayment addRepayment(Repayment repayment);

    Repayment updateRepayment(Repayment repayment);

    void deleteRepayment(Long id);

    Repayment getRepaymentById(Long id);

    List<Repayment> getAllRepayments();
    Repayment payCredit(Long creditId, Repayment repayment);

    BigDecimal getRemainingAmount(Long creditId);
    List<Repayment> getRepaymentsByCreditId(Long creditId);
    List<Repayment> getRepaymentsByClientEmail(String email);
}
