package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Repayment;
import org.example.projet_pi.entity.RepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    // ✅ Méthodes existantes
    List<Repayment> findByCreditCreditIdOrderByPaymentDateAsc(Long creditId);
    List<Repayment> findByCredit_CreditId(Long creditId);
    List<Repayment> findByClient_Email(String email);

    @Query("""
        select sum(r.amount)
        from Repayment r
        where r.credit.creditId = :creditId
        and r.status = org.example.projet_pi.entity.RepaymentStatus.PAID
    """)
    BigDecimal sumPaidSuccess(@Param("creditId") Long creditId);

    @Query("SELECT r FROM Repayment r WHERE r.credit.creditId = :creditId AND r.status IN :statuses")
    List<Repayment> findByCreditIdAndStatusIn(
            @Param("creditId") Long creditId,
            @Param("statuses") List<RepaymentStatus> statuses);

    // 🔴 NOUVELLE MÉTHODE POUR STRIPE
    /**
     * Trouve un paiement Stripe par sa référence
     * @param reference La référence du paiement (ex: STRIPE-abc123)
     * @return Le paiement correspondant ou null
     */
    @Query("SELECT r FROM Repayment r WHERE r.reference = :reference")
    Repayment findByReference(@Param("reference") String reference);

    /**
     * Vérifie si un paiement Stripe existe déjà
     * @param reference La référence du paiement
     * @return true si le paiement existe
     */
    boolean existsByReference(String reference);
}