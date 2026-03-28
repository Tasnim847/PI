package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.RepaymentDTO;
import org.example.projet_pi.entity.Repayment;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public interface IRepaymentService {

    // ===== CRUD =====
    Repayment addRepayment(Repayment repayment);
    Repayment updateRepayment(Repayment repayment);
    void deleteRepayment(Long id);
    Repayment getRepaymentById(Long id);
    List<Repayment> getAllRepayments();

    // ===== METIER =====
    Repayment payCredit(Long creditId, Repayment repayment);
    BigDecimal getRemainingAmount(Long creditId);
    List<Repayment> getRepaymentsByCreditId(Long creditId);
    List<Repayment> getRepaymentsByClientEmail(String email);

    // ===== PDF =====
    byte[] generateAmortissementPdf(Long creditId) throws IOException;
    void sendAmortissementPdfByEmail(Long creditId) throws IOException;

    // ===== STRIPE =====
    /**
     * Crée un PaymentIntent Stripe pour un paiement de crédit
     * @param creditId ID du crédit
     * @param amount Montant à payer
     * @param currency Devise (ex: TND, EUR, USD)
     * @return RepaymentDTO contenant les informations du paiement Stripe
     */
    RepaymentDTO createStripePaymentIntent(Long creditId, BigDecimal amount, String currency);

    /**
     * Traite un paiement Stripe réussi (appelé par le webhook)
     * @param creditId ID du crédit
     * @param amount Montant payé
     */
    void processSuccessfulStripePayment(Long creditId, BigDecimal amount);
}