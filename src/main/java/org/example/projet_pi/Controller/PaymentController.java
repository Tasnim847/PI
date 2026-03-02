package org.example.projet_pi.Controller;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.PaymentDTO;
import org.example.projet_pi.Dto.PaymentRequestDTO;
import org.example.projet_pi.Mapper.PaymentMapper;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Repository.PaymentRepository;
import org.example.projet_pi.Service.IPaymentService;
import org.example.projet_pi.entity.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final IPaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final ClientRepository clientRepository;
    private final InsuranceContractRepository contractRepository;

    @PostMapping("/addPayment")
    public PaymentDTO addPayment(
            @RequestBody PaymentDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {
        return paymentService.addPayment(dto, currentUser.getUsername());
    }

    @GetMapping("/getPayment/{id}")
    public PaymentDTO getPaymentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return paymentService.getPaymentById(id, currentUser.getUsername());
    }

    @GetMapping("/allPayments")
    public List<PaymentDTO> getAllPayments(
            @AuthenticationPrincipal UserDetails currentUser) {
        return paymentService.getAllPayments(currentUser.getUsername());
    }

    @GetMapping("/contract/{contractId}")
    public List<PaymentDTO> getPaymentsByContract(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails currentUser) {
        return paymentService.getPaymentsByContractId(contractId, currentUser.getUsername());
    }

    // Endpoint Stripe pour créer un PaymentIntent
    @PostMapping("/create-payment-intent/{contractId}")
    public ResponseEntity<Map<String, String>> createPaymentIntent(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails currentUser) throws StripeException {

        // Vérifier d'abord les droits via le service
        paymentService.getPaymentsByContractId(contractId, currentUser.getUsername());

        PaymentIntent paymentIntent = paymentService.createStripePaymentIntent(contractId);

        Map<String, String> response = new HashMap<>();
        response.put("clientSecret", paymentIntent.getClientSecret());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload) {
        // Traiter le webhook Stripe
        // Récupérer le PaymentIntent et appeler handleSuccessfulPayment
        return ResponseEntity.ok("Webhook reçu");
    }

    @PostMapping("/payments")
    public ResponseEntity<?> makePayment(@RequestBody PaymentRequestDTO request) {

        Client client = clientRepository.findByEmail(request.getClientEmail())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // 1️⃣ Récupérer la tranche existante du contrat qui n'est pas encore payée
        List<Payment> tranches = paymentRepository
                .findAllByContract_ContractIdAndStatusOrderByPaymentDateAsc(
                        request.getContractId(),
                        PaymentStatus.PENDING
                );

        if (tranches.isEmpty()) {
            throw new RuntimeException("No pending payment found");
        }

// On prend la première tranche à payer
        Payment tranche = tranches.get(0);
        // 2️⃣ Vérifier la date ou autre logique si nécessaire
        Date now = new Date();
        // Optionnel : if(tranche.getDueDate().after(now)) { ... }

        // 3️⃣ Mettre à jour la tranche
        tranche.setAmount(request.getInstallmentAmount());
        tranche.setPaymentDate(now);
        tranche.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentType()));
        tranche.setStatus(PaymentStatus.PAID);

        // 4️⃣ Sauvegarder
        Payment savedTranche = paymentRepository.save(tranche);

        // 5️⃣ Mapper en DTO pour la réponse
        PaymentDTO response = PaymentMapper.toDTO(savedTranche);

        return ResponseEntity.ok(response);
    }
}