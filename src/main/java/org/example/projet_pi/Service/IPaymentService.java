package org.example.projet_pi.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.example.projet_pi.Dto.PaymentDTO;
import java.util.List;

public interface IPaymentService {

    // Méthodes avec sécurité
    PaymentDTO addPayment(PaymentDTO dto, String userEmail);

    PaymentDTO getPaymentById(Long id, String userEmail);

    List<PaymentDTO> getAllPayments(String userEmail);

    List<PaymentDTO> getPaymentsByContractId(Long contractId, String userEmail);

    // Méthodes Stripe (sans sécurité)
    PaymentIntent createStripePaymentIntent(Long contractId) throws StripeException;

    void handleSuccessfulPayment(String stripePaymentId, Long amountInCents);

    // Méthodes non utilisées (gardées pour compatibilité)
    PaymentDTO updatePayment(PaymentDTO dto);

    void deletePayment(Long id);
}