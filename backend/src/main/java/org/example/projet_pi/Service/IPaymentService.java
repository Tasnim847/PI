package org.example.projet_pi.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.example.projet_pi.Dto.PaymentDTO;
import java.util.List;
import java.util.Map;

public interface IPaymentService {

    // Méthodes avec sécurité
    PaymentDTO addPayment(PaymentDTO dto, String userEmail);

    PaymentDTO getPaymentById(Long id, String userEmail);

    List<PaymentDTO> getAllPayments(String userEmail);

    List<PaymentDTO> getPaymentsByContractId(Long contractId, String userEmail);

    // Méthodes Stripe
    PaymentIntent createStripePaymentIntent(Long contractId) throws StripeException;

    void handleSuccessfulPayment(String stripePaymentId, Long amountInCents, Long contractId);

    void handleSuccessfulPayment(String stripePaymentId, Long amountInCents);

    // ✅ NOUVELLES MÉTHODES
    PaymentDTO updatePayment(PaymentDTO dto);
    void deletePayment(Long id);

    // ✅ NOUVELLES MÉTHODES UTILES
    Map<String, Object> getRemainingBalance(Long contractId, String userEmail);

    List<Map<String, Object>> getPaymentHistory(Long contractId, String userEmail);

    Map<String, Object> getPaymentIntentStatus(String paymentIntentId);

    boolean cancelStripePaymentIntent(String paymentIntentId);

    List<Map<String, Object>> getRemainingInstallments(Long contractId, String userEmail);

    PaymentDTO processManualPayment(Long contractId, double amount, String paymentMethod, String userEmail);

}