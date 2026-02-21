package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Repayment;
import org.example.projet_pi.entity.RepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    List<Repayment> findByCreditCreditIdOrderByPaymentDateAsc(Long creditId);

    @Query("""
        select sum(r.amount)
        from Repayment r
        where r.credit.creditId = :creditId
        and r.status = org.example.projet_pi.entity.RepaymentStatus.PAID
    """)
    BigDecimal sumPaidSuccess(@Param("creditId") Long creditId);

}