package org.example.projet_pi.Controller;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Dto.PaymentDTO;
import org.example.projet_pi.Dto.PaymentRequestDTO;
import org.example.projet_pi.Mapper.PaymentMapper;
import org.example.projet_pi.Repository.*;
import org.example.projet_pi.Service.EmailService;
import org.example.projet_pi.Service.IPaymentService;
import org.example.projet_pi.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

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
        return ResponseEntity.ok("Webhook reçu");
    }

    // ==================== MANUAL PAYMENT ====================

    @PostMapping("/payments")
    public ResponseEntity<?> makePayment(@RequestBody PaymentRequestDTO request) {

        // Pour CASH : ne pas traiter le paiement immédiatement
        if ("CASH".equalsIgnoreCase(request.getPaymentType())) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "PENDING_APPROVAL");
            response.put("message", "Demande de paiement en espèces envoyée à l'agent");
            response.put("contractId", request.getContractId());
            response.put("amount", request.getInstallmentAmount());
            return ResponseEntity.ok(response);
        }

        // Pour les autres méthodes (CARD, BANK_TRANSFER) : traitement normal
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

        // ✅ CORRECTION : Vérifier que TOUTES les tranches sont payées
        long remainingPendingPayments = contract.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .count();

        if (remainingPendingPayments == 0 && contract.getRemainingAmount() <= 0.01) {
            contract.setStatus(ContractStatus.COMPLETED);
            contract.setRemainingAmount(0.0);
            log.info("🎉 Contrat {} complété - toutes les tranches sont payées", contract.getContractId());
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
        result.put("remainingInstallments", remainingPendingPayments);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/process-approved-cash")
    public ResponseEntity<?> processApprovedCashPayment(@RequestBody PaymentRequestDTO request) {
        log.info("💰 Traitement paiement CASH approuvé pour le contrat {}", request.getContractId());

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
        tranche.setPaymentMethod(PaymentMethod.CASH);
        tranche.setStatus(PaymentStatus.PAID);

        contract.setTotalPaid(contract.getTotalPaid() + amountToPay);
        contract.setRemainingAmount(contract.getRemainingAmount() - amountToPay);

        // ✅ CORRECTION : Vérifier que TOUTES les tranches sont payées
        long remainingPendingPayments = contract.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .count();

        if (remainingPendingPayments == 0 && contract.getRemainingAmount() <= 0.01) {
            contract.setStatus(ContractStatus.COMPLETED);
            contract.setRemainingAmount(0.0);
            log.info("🎉 Contrat {} complété - toutes les tranches sont payées", contract.getContractId());
        } else {
            log.info("📊 Contrat {} - Tranche payée. Reste: {} DT, Tranches restantes: {}",
                    contract.getContractId(),
                    contract.getRemainingAmount(),
                    remainingPendingPayments);
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
        result.put("remainingInstallments", remainingPendingPayments);
        result.put("message", "Paiement en espèces effectué avec succès");

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

            long remainingPendingPayments = contract.getPayments().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                    .count();

            Map<String, Object> response = new HashMap<>();
            response.put("payment", payment);
            response.put("remainingAmount", contract.getRemainingAmount());
            response.put("totalPaid", contract.getTotalPaid());
            response.put("contractStatus", contract.getStatus().name());
            response.put("remainingInstallments", remainingPendingPayments);
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

    @PostMapping("/confirm-payment/{paymentIntentId}")
    public ResponseEntity<Map<String, Object>> confirmPayment(
            @PathVariable String paymentIntentId,
            @AuthenticationPrincipal UserDetails currentUser) throws StripeException {

        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", paymentIntent.getStatus());
        response.put("paymentIntentId", paymentIntent.getId());

        if ("succeeded".equals(paymentIntent.getStatus())) {
            String contractIdStr = paymentIntent.getMetadata().get("contractId");
            if (contractIdStr != null) {
                Long contractId = Long.parseLong(contractIdStr);
                paymentService.handleSuccessfulPayment(paymentIntentId, paymentIntent.getAmount(), contractId);
                response.put("message", "Paiement confirmé avec succès");
            }
        }

        return ResponseEntity.ok(response);
    }

    // Dans PaymentController.java

    /**
     * Vérifier le solde d'un compte par RIP
     */
    // Dans PaymentController.java - Modifier checkBankBalance

    /**
     * Vérifier le solde d'un compte par RIP (21 chiffres)
     */
    @GetMapping("/check-balance/{rip}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Map<String, Object>> checkBankBalance(
            @PathVariable String rip,
            @RequestParam Double amount,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Valider que le RIP a exactement 21 chiffres
            if (rip == null || !rip.matches("\\d{21}")) {
                throw new RuntimeException("Le RIP doit contenir exactement 21 chiffres");
            }

            Client client = clientRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));

            // Trouver le compte par RIP
            Account account = accountRepository.findByRip(rip)
                    .orElseThrow(() -> new RuntimeException("Compte non trouvé pour ce RIP"));

            // Vérifier que le compte appartient au client
            if (!account.getClient().getId().equals(client.getId())) {
                throw new RuntimeException("Ce compte ne vous appartient pas");
            }

            // Vérifier que le compte est actif
            if (!"ACTIVE".equals(account.getStatus())) {
                throw new RuntimeException("Votre compte n'est pas actif");
            }

            double currentBalance = account.getBalance();
            boolean sufficient = currentBalance >= amount;

            response.put("success", true);
            response.put("balance", currentBalance);
            response.put("sufficient", sufficient);
            response.put("requiredAmount", amount);
            response.put("shortage", sufficient ? 0 : amount - currentBalance);

        } catch (Exception e) {
            log.error("❌ Erreur vérification solde: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Paiement par virement bancaire (Bank Transfer)
     */
    @PostMapping("/pay-by-bank-transfer")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> payByBankTransfer(@RequestBody PaymentRequestDTO request,
                                               @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Valider le RIP (21 chiffres)
            String sourceRip = request.getSourceRip();
            if (sourceRip == null || !sourceRip.matches("\\d{21}")) {
                throw new RuntimeException("Le RIP source doit contenir exactement 21 chiffres");
            }

            Client client = clientRepository.findByEmail(request.getClientEmail())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));

            // Vérifier le contrat
            InsuranceContract contract = contractRepository.findById(request.getContractId())
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

            // Vérifier que le contrat appartient au client
            if (!contract.getClient().getId().equals(client.getId())) {
                throw new RuntimeException("Ce contrat ne vous appartient pas");
            }

            // Vérifier qu'il y a des paiements en attente
            List<Payment> pendingPayments = paymentRepository
                    .findAllByContract_ContractIdAndStatusOrderByPaymentDateAsc(
                            request.getContractId(),
                            PaymentStatus.PENDING
                    );

            if (pendingPayments.isEmpty()) {
                throw new RuntimeException("Aucune tranche en attente");
            }

            Payment pendingPayment = pendingPayments.get(0);

            // Vérifier le montant
            double amountToPay = request.getInstallmentAmount();
            if (Math.abs(amountToPay - pendingPayment.getAmount()) > 0.01) {
                throw new RuntimeException("Le montant ne correspond pas à la tranche due");
            }

            // Récupérer le compte source par RIP
            Account sourceAccount = accountRepository.findByRip(sourceRip)
                    .orElseThrow(() -> new RuntimeException("Compte source non trouvé pour ce RIP"));

            // Vérifier que le compte appartient au client
            if (!sourceAccount.getClient().getId().equals(client.getId())) {
                throw new RuntimeException("Ce compte ne vous appartient pas");
            }

            // Vérifier que le compte est actif
            if (!"ACTIVE".equals(sourceAccount.getStatus())) {
                throw new RuntimeException("Votre compte n'est pas actif");
            }

            // Vérifier le solde
            if (sourceAccount.getBalance() < amountToPay) {
                throw new RuntimeException(String.format(
                        "Solde insuffisant. Solde: %.2f DT, Montant requis: %.2f DT",
                        sourceAccount.getBalance(), amountToPay
                ));
            }

            // ✅ DÉBITER LE COMPTE
            Transaction debitTransaction = new Transaction();
            debitTransaction.setAccount(sourceAccount);
            debitTransaction.setAmount(amountToPay);
            debitTransaction.setType("WITHDRAW");
            debitTransaction.setDate(LocalDate.now());
            debitTransaction.setDescription(String.format("Paiement d'assurance - Contrat #%d - Tranche #%d",
                    contract.getContractId(), pendingPayment.getPaymentId()));

            // Mettre à jour le solde
            sourceAccount.setBalance(sourceAccount.getBalance() - amountToPay);
            accountRepository.save(sourceAccount);

            // Marquer la tranche comme payée
            pendingPayment.setStatus(PaymentStatus.PAID);
            pendingPayment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
            pendingPayment.setPaymentDate(new Date());
            paymentRepository.save(pendingPayment);

            // Mettre à jour le contrat
            contract.setTotalPaid(contract.getTotalPaid() + amountToPay);
            contract.setRemainingAmount(contract.getRemainingAmount() - amountToPay);

            // Vérifier si toutes les tranches sont payées
            long remainingPendingPayments = contract.getPayments().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                    .count();

            if (remainingPendingPayments == 0 && contract.getRemainingAmount() <= 0.01) {
                contract.setStatus(ContractStatus.COMPLETED);
                contract.setRemainingAmount(0.0);
                log.info("🎉 Contrat {} complété - toutes les tranches sont payées", contract.getContractId());
            }

            contractRepository.save(contract);
            transactionRepository.save(debitTransaction);

            // Envoyer email de confirmation
            try {
                emailService.sendPaymentConfirmationEmail(client, contract, pendingPayment);
                log.info("📧 Email de confirmation de paiement par virement envoyé à {}", client.getEmail());
            } catch (Exception e) {
                log.error("❌ Erreur envoi email: {}", e.getMessage());
            }

            response.put("success", true);
            response.put("message", "Paiement par virement bancaire effectué avec succès");
            response.put("paymentId", pendingPayment.getPaymentId());
            response.put("amountPaid", amountToPay);
            response.put("newBalance", sourceAccount.getBalance());
            response.put("remainingAmount", contract.getRemainingAmount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur paiement par virement: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}