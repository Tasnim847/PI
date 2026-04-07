package org.example.projet_pi.Controller;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Dto.PaymentDTO;
import org.example.projet_pi.Dto.PaymentRequestDTO;
import org.example.projet_pi.Mapper.PaymentMapper;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Repository.PaymentRepository;
import org.example.projet_pi.Service.EmailService;
import org.example.projet_pi.Service.IPaymentService;
import org.example.projet_pi.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final IPaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final ClientRepository clientRepository;
    private final InsuranceContractRepository contractRepository;
    private final EmailService emailService;

    // ==================== BASIC CRUD ====================

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

    // ==================== STRIPE PAYMENT ====================

    @PostMapping("/create-payment-intent/{contractId}")
    public ResponseEntity<Map<String, String>> createPaymentIntent(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails currentUser) throws StripeException {

        paymentService.getPaymentsByContractId(contractId, currentUser.getUsername());

        PaymentIntent paymentIntent = paymentService.createStripePaymentIntent(contractId);

        Map<String, String> response = new HashMap<>();
        response.put("clientSecret", paymentIntent.getClientSecret());
        response.put("paymentIntentId", paymentIntent.getId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload) {
        // Traiter le webhook Stripe
        return ResponseEntity.ok("Webhook reçu");
    }

    // ==================== MANUAL PAYMENT ====================

    @PostMapping("/payments")
    public ResponseEntity<?> makePayment(@RequestBody PaymentRequestDTO request) {

        Client client = clientRepository.findByEmail(request.getClientEmail())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        List<Payment> tranches = paymentRepository
                .findAllByContract_ContractIdAndStatusOrderByPaymentDateAsc(
                        request.getContractId(),
                        PaymentStatus.PENDING
                );

        if (tranches.isEmpty()) {
            throw new RuntimeException("No pending payment found");
        }

        Payment tranche = tranches.get(0);

        InsuranceContract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        double amountToPay = request.getInstallmentAmount();
        if (Math.abs(amountToPay - tranche.getAmount()) > 0.01) {
            throw new RuntimeException("Le montant ne correspond pas à la tranche due");
        }

        if (amountToPay > contract.getRemainingAmount()) {
            throw new RuntimeException("Le montant dépasse le reste à payer");
        }

        tranche.setAmount(amountToPay);
        tranche.setPaymentDate(new Date());
        tranche.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentType()));
        tranche.setStatus(PaymentStatus.PAID);

        contract.setTotalPaid(contract.getTotalPaid() + amountToPay);
        contract.setRemainingAmount(contract.getRemainingAmount() - amountToPay);

        if (contract.getRemainingAmount() <= 0.01) {
            contract.setStatus(ContractStatus.COMPLETED);
            contract.setRemainingAmount(0.0);
        }

        Payment savedTranche = paymentRepository.save(tranche);
        contractRepository.save(contract);

        try {
            emailService.sendPaymentConfirmationEmail(client, contract, savedTranche);
            log.info("📧 Email de confirmation envoyé à {}", client.getEmail());
        } catch (Exception e) {
            log.error("❌ Erreur envoi email: {}", e.getMessage());
        }

        PaymentDTO response = PaymentMapper.toDTO(savedTranche);

        Map<String, Object> result = new HashMap<>();
        result.put("payment", response);
        result.put("remainingAmount", contract.getRemainingAmount());
        result.put("totalPaid", contract.getTotalPaid());
        result.put("status", contract.getStatus().name());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/manual-payment/{contractId}")
    public ResponseEntity<?> manualPayment(
            @PathVariable Long contractId,
            @RequestParam Double amount,
            @RequestParam String paymentMethod,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            PaymentDTO payment = paymentService.processManualPayment(
                    contractId, amount, paymentMethod, currentUser.getUsername());

            InsuranceContract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

            Map<String, Object> response = new HashMap<>();
            response.put("payment", payment);
            response.put("remainingAmount", contract.getRemainingAmount());
            response.put("totalPaid", contract.getTotalPaid());
            response.put("contractStatus", contract.getStatus().name());
            response.put("message", "Paiement effectué avec succès");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors du paiement manuel: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== PAYMENT STATUS & UTILITIES ====================

    @GetMapping("/status/{contractId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails currentUser) {

        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        List<PaymentDTO> payments = paymentService.getPaymentsByContractId(contractId, currentUser.getUsername());

        Map<String, Object> status = new HashMap<>();
        status.put("contractId", contractId);
        status.put("totalPremium", contract.getPremium());
        status.put("remainingAmount", contract.getRemainingAmount());
        status.put("totalPaid", contract.getTotalPaid());
        status.put("installmentAmount", contract.calculateInstallmentAmount());
        status.put("payments", payments);
        status.put("status", contract.getStatus().name());

        return ResponseEntity.ok(status);
    }

    @GetMapping("/remaining-balance/{contractId}")
    public ResponseEntity<Map<String, Object>> getRemainingBalance(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            Map<String, Object> balance = paymentService.getRemainingBalance(contractId, currentUser.getUsername());
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history/{contractId}")
    public ResponseEntity<List<Map<String, Object>>> getPaymentHistory(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            List<Map<String, Object>> history = paymentService.getPaymentHistory(contractId, currentUser.getUsername());
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/stripe/status/{paymentIntentId}")
    public ResponseEntity<Map<String, Object>> getStripePaymentStatus(
            @PathVariable String paymentIntentId,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            Map<String, Object> status = paymentService.getPaymentIntentStatus(paymentIntentId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/stripe/cancel/{paymentIntentId}")
    public ResponseEntity<Map<String, Object>> cancelStripePayment(
            @PathVariable String paymentIntentId,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            boolean cancelled = paymentService.cancelStripePaymentIntent(paymentIntentId);
            Map<String, Object> response = new HashMap<>();
            response.put("cancelled", cancelled);
            response.put("paymentIntentId", paymentIntentId);
            response.put("message", cancelled ? "Paiement annulé avec succès" : "Impossible d'annuler le paiement");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/installments/{contractId}")
    public ResponseEntity<List<Map<String, Object>>> getRemainingInstallments(
            @PathVariable Long contractId,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            List<Map<String, Object>> installments = paymentService.getRemainingInstallments(contractId, currentUser.getUsername());
            return ResponseEntity.ok(installments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // ==================== UPDATE & DELETE (NON SUPPORTÉS) ====================

    @PutMapping("/updatePayment")
    public ResponseEntity<Map<String, String>> updatePayment(@RequestBody PaymentDTO dto) {
        try {
            paymentService.updatePayment(dto);
            return ResponseEntity.ok(Map.of("message", "Paiement modifié avec succès"));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/deletePayment/{id}")
    public ResponseEntity<Map<String, String>> deletePayment(@PathVariable Long id) {
        try {
            paymentService.deletePayment(id);
            return ResponseEntity.ok(Map.of("message", "Paiement supprimé avec succès"));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}