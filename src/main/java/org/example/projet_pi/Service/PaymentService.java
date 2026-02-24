package org.example.projet_pi.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.PaymentDTO;
import org.example.projet_pi.Mapper.PaymentMapper;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Repository.PaymentRepository;
import org.example.projet_pi.entity.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PaymentService implements IPaymentService {

    private final PaymentRepository paymentRepository;
    private final InsuranceContractRepository contractRepository;

    // Création PaymentIntent Stripe
    public PaymentIntent createStripePaymentIntent(Long contractId) throws StripeException {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        long amountInCents = (long) (contract.getRemainingAmount() * 100); // convert DT -> centimes

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        return PaymentIntent.create(params);
    }

    // Méthode pour gérer un paiement réussi (webhook)
    public void handleSuccessfulPayment(String stripePaymentId, Long amountInCents) {
        double amountDT = amountInCents / 100.0;

        PaymentDTO dto = new PaymentDTO();
        dto.setAmount(amountDT);
        dto.setPaymentDate(new Date());
        dto.setPaymentMethod("CREDIT_CARD"); // Paiement Stripe
        dto.setStatus("PAID");

        // ⚡ Tu peux associer à ton contrat ici
        // Si tu veux, il faudrait retrouver le contrat correspondant au PaymentIntent
        // Par exemple via metadata de PaymentIntent
        System.out.println("Paiement Stripe réussi : " + stripePaymentId + ", montant : " + amountDT);
    }

    @Override
    public PaymentDTO addPayment(PaymentDTO dto) {
        InsuranceContract contract = contractRepository.findById(dto.getContractId())
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        Payment payment = PaymentMapper.toEntity(dto);
        payment.setContract(contract);
        payment.setPaymentDate(new Date());
        payment.setStatus(PaymentStatus.PAID);

        contract.applyPayment(payment.getAmount());

        payment = paymentRepository.save(payment);
        contractRepository.save(contract);

        return PaymentMapper.toDTO(payment);
    }

    @Override
    public PaymentDTO updatePayment(PaymentDTO dto) { throw new RuntimeException("Modification paiement interdite"); }

    @Override
    public void deletePayment(Long id) { throw new RuntimeException("Suppression paiement interdite"); }

    @Override
    public PaymentDTO getPaymentById(Long id) {
        return PaymentMapper.toDTO(paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé")));
    }

    @Override
    public List<PaymentDTO> getAllPayments() {
        return paymentRepository.findAll().stream().map(PaymentMapper::toDTO).collect(Collectors.toList());
    }

    // Dans PaymentService.java - Ajoutez cette méthode
    private void checkAndMarkContractAsCompletedAfterPayment(InsuranceContract contract) {
        Date today = new Date();
        Date endDate = contract.getEndDate();

        if (endDate != null && endDate.before(today)) {
            boolean allPaymentsPaid = contract.getPayments().stream()
                    .allMatch(p -> p.getStatus() == PaymentStatus.PAID);

            if (allPaymentsPaid && Math.abs(contract.getTotalPaid() - contract.getPremium()) < 0.01) {
                contract.setStatus(ContractStatus.COMPLETED);
                contractRepository.save(contract);
                System.out.println("🎉 Contrat " + contract.getContractId() +
                        " marqué COMPLETED après paiement");
            }
        }
    }
}