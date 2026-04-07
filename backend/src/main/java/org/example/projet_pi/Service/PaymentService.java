package org.example.projet_pi.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Dto.PaymentDTO;
import org.example.projet_pi.Mapper.PaymentMapper;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Repository.PaymentRepository;
import org.example.projet_pi.Repository.UserRepository;
import org.example.projet_pi.entity.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService implements IPaymentService {

    private final PaymentRepository paymentRepository;
    private final InsuranceContractRepository contractRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ==================== MÉTHODES PRINCIPALES ====================

    @Override
    @Transactional
    public PaymentDTO addPayment(PaymentDTO dto, String userEmail) {
        // 1. Récupérer l'utilisateur connecté
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 2. Récupérer le contrat
        InsuranceContract contract = contractRepository.findById(dto.getContractId())
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        // 3. Vérification des droits d'accès
        if (user instanceof Client) {
            if (!contract.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous ne pouvez payer que vos propres contrats");
            }
        } else if (user instanceof AgentAssurance) {
            AgentAssurance agent = (AgentAssurance) user;
            if (!contract.getClient().getAgentAssurance().getId().equals(agent.getId())) {
                throw new AccessDeniedException("Ce contrat n'appartient pas à un de vos clients");
            }
        }

        // 4. Vérifier si le contrat n'est pas déjà COMPLETED
        if (contract.getStatus() == ContractStatus.COMPLETED) {
            throw new RuntimeException("Ce contrat est déjà complété, aucun paiement supplémentaire n'est requis");
        }

        // 5. Vérifier si ce paiement n'a pas déjà été traité par Stripe
        boolean alreadyProcessedByStripe = contract.getPayments().stream()
                .anyMatch(p -> Math.abs(dto.getAmount() - p.getAmount()) < 0.01 &&
                        p.getPaymentMethod() == PaymentMethod.CARD &&
                        p.getPaymentDate() != null &&
                        p.getPaymentDate().getTime() > System.currentTimeMillis() - 120000);

        if (alreadyProcessedByStripe) {
            log.warn("⚠️ Paiement déjà traité par Stripe pour le contrat {}", contract.getContractId());
            throw new RuntimeException("Ce paiement a déjà été traité");
        }

        // 6. Vérifier que le montant ne dépasse pas le reste à payer
        if (dto.getAmount() > contract.getRemainingAmount()) {
            throw new RuntimeException("Le montant du paiement (" + dto.getAmount() +
                    ") dépasse le reste à payer (" + contract.getRemainingAmount() + ")");
        }

        // 7. Vérifier que le montant n'est pas trop petit
        if (dto.getAmount() < 0.01) {
            throw new RuntimeException("Le montant du paiement est trop petit");
        }

        // 8. Créer le paiement
        Payment payment = PaymentMapper.toEntity(dto);
        payment.setContract(contract);
        payment.setPaymentDate(new Date());
        payment.setStatus(PaymentStatus.PAID);

        // 9. Appliquer le paiement au contrat
        double oldRemaining = contract.getRemainingAmount();
        contract.applyPayment(payment.getAmount());

        // 10. Sauvegarder
        payment = paymentRepository.save(payment);
        contractRepository.save(contract);

        // 11. Vérifier si le contrat est maintenant COMPLETED
        checkAndMarkContractAsCompletedAfterPayment(contract);

        log.info("✅ Paiement ajouté: {} DT pour le contrat {} (reste: {} -> {} DT)",
                payment.getAmount(), contract.getContractId(), oldRemaining, contract.getRemainingAmount());

        // 12. Envoyer email de confirmation
        try {
            emailService.sendPaymentConfirmationEmail(contract.getClient(), contract, payment);
            log.info("📧 Email de confirmation envoyé à {}", contract.getClient().getEmail());
        } catch (Exception e) {
            log.error("❌ Erreur envoi email confirmation: {}", e.getMessage());
        }

        return PaymentMapper.toDTO(payment);
    }

    // ==================== MÉTHODES STRIPE ====================

    @Override
    public PaymentIntent createStripePaymentIntent(Long contractId) throws StripeException {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        // Vérifier que le contrat n'est pas déjà COMPLETED
        if (contract.getStatus() == ContractStatus.COMPLETED) {
            throw new RuntimeException("Ce contrat est déjà complété");
        }

        // Par défaut, proposer le montant de l'échéance (tranche)
        double installmentAmount = contract.calculateInstallmentAmount();
        long amountInCents = (long) (installmentAmount * 100);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contractId", contractId.toString());
        metadata.put("clientEmail", contract.getClient().getEmail());
        metadata.put("paymentType", "INSTALLMENT");
        metadata.put("installmentAmount", String.valueOf(installmentAmount));
        metadata.put("remainingAmount", String.valueOf(contract.getRemainingAmount()));

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .putAllMetadata(metadata)
                .build();

        log.info("✅ PaymentIntent créé pour la tranche de {} DT du contrat {}",
                installmentAmount, contractId);
        return PaymentIntent.create(params);
    }

    public PaymentIntent createCustomStripePaymentIntent(Long contractId, Double amount) throws StripeException {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        if (contract.getStatus() == ContractStatus.COMPLETED) {
            throw new RuntimeException("Ce contrat est déjà complété");
        }

        if (amount > contract.getRemainingAmount()) {
            throw new RuntimeException("Le montant demandé (" + amount +
                    ") dépasse le reste à payer (" + contract.getRemainingAmount() + ")");
        }

        if (amount <= 0) {
            throw new RuntimeException("Le montant doit être supérieur à 0");
        }

        long amountInCents = (long) (amount * 100);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contractId", contractId.toString());
        metadata.put("clientEmail", contract.getClient().getEmail());
        metadata.put("paymentType", "CUSTOM");
        metadata.put("requestedAmount", String.valueOf(amount));
        metadata.put("remainingAmount", String.valueOf(contract.getRemainingAmount()));

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .putAllMetadata(metadata)
                .build();

        log.info("✅ PaymentIntent personnalisé créé: {} DT pour le contrat {}", amount, contractId);
        return PaymentIntent.create(params);
    }

    @Override
    @Transactional
    public void handleSuccessfulPayment(String stripePaymentId, Long amountInCents, Long contractId) {
        try {
            log.info("🔄 Traitement du paiement Stripe: {} pour le contrat {}", stripePaymentId, contractId);

            InsuranceContract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé: " + contractId));

            double amountDT = amountInCents / 100.0;

            // ✅ Récupérer la première tranche non payée
            Payment pendingPayment = contract.getPayments().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                    .min(Comparator.comparing(Payment::getPaymentDate))
                    .orElseThrow(() -> new RuntimeException("Aucune tranche à payer"));

            // Vérifier que le montant correspond à la tranche
            if (Math.abs(amountDT - pendingPayment.getAmount()) > 0.01) {
                log.warn("⚠️ Montant reçu {} DT ne correspond pas à la tranche {} DT",
                        amountDT, pendingPayment.getAmount());
            }

            log.info("💰 Paiement de la tranche {} ({} DT)", pendingPayment.getPaymentId(), amountDT);
            log.info("📊 Reste AVANT paiement: {}", contract.getRemainingAmount());

            // Marquer la tranche comme payée
            pendingPayment.setStatus(PaymentStatus.PAID);
            pendingPayment.setPaymentMethod(PaymentMethod.CARD);
            pendingPayment.setPaymentDate(new Date());
            paymentRepository.save(pendingPayment);

            // Mettre à jour le contrat
            contract.setRemainingAmount(contract.getRemainingAmount() - amountDT);
            contract.setTotalPaid(contract.getTotalPaid() + amountDT);
            contractRepository.save(contract);

            log.info("✅ Paiement Stripe {} traité pour la tranche {} du contrat {}",
                    stripePaymentId, pendingPayment.getPaymentId(), contractId);
            log.info("💰 Reste à payer: {} DT", contract.getRemainingAmount());

            // Vérifier si toutes les tranches sont payées
            boolean allPaid = contract.getPayments().stream()
                    .allMatch(p -> p.getStatus() == PaymentStatus.PAID);

            if (allPaid && contract.getRemainingAmount() < 0.01) {
                contract.setStatus(ContractStatus.COMPLETED);
                contractRepository.save(contract);
                log.info("🎉 Contrat {} complété!", contractId);

                emailService.sendContractCompletedEmail(contract.getClient(), contract);
            } else {
                // Envoyer email de confirmation pour la tranche payée
                emailService.sendPaymentConfirmationEmail(contract.getClient(), contract, pendingPayment);
            }

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur traitement paiement: " + e.getMessage());
        }
    }

    @Override
    public void handleSuccessfulPayment(String stripePaymentId, Long amountInCents) {
        log.warn("⚠️ Appel à la méthode de fallback handleSuccessfulPayment sans contractId");
        log.info("Paiement Stripe reçu: {} pour {} centimes", stripePaymentId, amountInCents);
    }

    // ==================== MÉTHODES DE CONSULTATION ====================

    @Override
    public PaymentDTO getPaymentById(Long id, String userEmail) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        InsuranceContract contract = payment.getContract();

        if (user instanceof Client) {
            if (!contract.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous ne pouvez consulter que vos propres paiements");
            }
        } else if (user instanceof AgentAssurance) {
            AgentAssurance agent = (AgentAssurance) user;
            if (!contract.getClient().getAgentAssurance().getId().equals(agent.getId())) {
                throw new AccessDeniedException("Ce paiement n'appartient pas à un de vos clients");
            }
        }

        return PaymentMapper.toDTO(payment);
    }

    @Override
    public List<PaymentDTO> getAllPayments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user instanceof Client) {
            return paymentRepository.findByContract_ClientId(user.getId()).stream()
                    .map(PaymentMapper::toDTO).toList();
        }

        if (user instanceof AgentAssurance agent) {
            return paymentRepository.findByContract_AgentAssuranceId(agent.getId()).stream()
                    .map(PaymentMapper::toDTO).toList();
        }

        return paymentRepository.findAll().stream()
                .map(PaymentMapper::toDTO).toList();
    }

    @Override
    public List<PaymentDTO> getPaymentsByContractId(Long contractId, String userEmail) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user instanceof Client) {
            if (!contract.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Ce contrat ne vous appartient pas");
            }
        } else if (user instanceof AgentAssurance) {
            AgentAssurance agent = (AgentAssurance) user;
            if (!contract.getClient().getAgentAssurance().getId().equals(agent.getId())) {
                throw new AccessDeniedException("Ce contrat n'appartient pas à un de vos clients");
            }
        }

        return contract.getPayments().stream()
                .map(PaymentMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Vérifier le solde restant d'un contrat
     */
    public Map<String, Object> getRemainingBalance(Long contractId, String userEmail) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user instanceof Client && !contract.getClient().getId().equals(user.getId())) {
            throw new AccessDeniedException("Ce contrat ne vous appartient pas");
        }

        Map<String, Object> balance = new HashMap<>();
        balance.put("contractId", contractId);
        balance.put("totalPremium", contract.getPremium());
        balance.put("totalPaid", contract.getTotalPaid());
        balance.put("remainingAmount", contract.getRemainingAmount());
        balance.put("nextInstallmentAmount", contract.calculateInstallmentAmount());
        balance.put("installmentsLeft", (int) Math.ceil(contract.getRemainingAmount() / contract.calculateInstallmentAmount()));
        balance.put("status", contract.getStatus().name());
        balance.put("startDate", contract.getStartDate());
        balance.put("endDate", contract.getEndDate());

        return balance;
    }

    /**
     * Récupérer l'historique des paiements avec détails
     */
    public List<Map<String, Object>> getPaymentHistory(Long contractId, String userEmail) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user instanceof Client && !contract.getClient().getId().equals(user.getId())) {
            throw new AccessDeniedException("Ce contrat ne vous appartient pas");
        }

        return contract.getPayments().stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                .map(payment -> {
                    Map<String, Object> paymentInfo = new HashMap<>();
                    paymentInfo.put("id", payment.getPaymentId());
                    paymentInfo.put("amount", payment.getAmount());
                    paymentInfo.put("date", payment.getPaymentDate());
                    paymentInfo.put("method", payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "N/A");
                    paymentInfo.put("status", payment.getStatus().name());
                    return paymentInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Recalculer les tranches de paiement restantes pour un contrat
     */
    public List<Map<String, Object>> getRemainingInstallments(Long contractId, String userEmail) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user instanceof Client && !contract.getClient().getId().equals(user.getId())) {
            throw new AccessDeniedException("Ce contrat ne vous appartient pas");
        }

        double remainingAmount = contract.getRemainingAmount();
        double installmentAmount = contract.calculateInstallmentAmount();
        int remainingInstallments = (int) Math.ceil(remainingAmount / installmentAmount);

        List<Map<String, Object>> installments = new ArrayList<>();

        for (int i = 1; i <= remainingInstallments; i++) {
            Map<String, Object> installment = new HashMap<>();
            installment.put("number", i);
            installment.put("amount", Math.min(installmentAmount, remainingAmount - (i - 1) * installmentAmount));
            installment.put("dueDate", calculateDueDate(contract.getStartDate(), i));
            installment.put("remainingAfter", Math.max(0, remainingAmount - i * installmentAmount));
            installments.add(installment);
        }

        return installments;
    }

    // ==================== MÉTHODES STRIPE UTILITAIRES ====================

    /**
     * Vérifier le statut d'un PaymentIntent Stripe
     */
    public Map<String, Object> getPaymentIntentStatus(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", paymentIntent.getId());
            response.put("status", paymentIntent.getStatus());
            response.put("amount", paymentIntent.getAmount() / 100.0);
            response.put("currency", paymentIntent.getCurrency());
            response.put("created", new Date(paymentIntent.getCreated() * 1000L));
            response.put("metadata", paymentIntent.getMetadata());

            log.info("📊 Statut du paiement Stripe {}: {}", paymentIntentId, paymentIntent.getStatus());

            return response;
        } catch (StripeException e) {
            log.error("❌ Erreur lors de la récupération du PaymentIntent: {}", e.getMessage());
            throw new RuntimeException("Erreur Stripe: " + e.getMessage());
        }
    }

    /**
     * Annuler un PaymentIntent Stripe (si non encore traité)
     */
    public boolean cancelStripePaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            String status = paymentIntent.getStatus();
            if ("requires_payment_method".equals(status) ||
                    "requires_confirmation".equals(status) ||
                    "requires_action".equals(status)) {

                PaymentIntent canceledIntent = paymentIntent.cancel();
                log.info("✅ PaymentIntent {} annulé avec succès (statut: {} -> {})",
                        paymentIntentId, status, canceledIntent.getStatus());
                return true;
            } else {
                log.warn("⚠️ Impossible d'annuler le PaymentIntent {} (statut: {})",
                        paymentIntentId, status);
                return false;
            }
        } catch (StripeException e) {
            log.error("❌ Erreur lors de l'annulation du PaymentIntent: {}", e.getMessage());
            return false;
        }
    }

    // ==================== MÉTHODES INTERNES ====================

    /**
     * Méthode interne pour créer un paiement sans passer par addPayment
     * (pour éviter la récursion avec le webhook Stripe)
     */
    @Transactional
    protected Payment createPaymentInternal(InsuranceContract contract, double amount, String paymentMethod) {
        // Validation
        if (contract == null) {
            throw new IllegalArgumentException("Le contrat ne peut pas être null");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à 0");
        }

        if (amount > contract.getRemainingAmount() + 0.01) {
            throw new IllegalArgumentException(
                    String.format("Le montant %.2f DT dépasse le reste à payer %.2f DT",
                            amount, contract.getRemainingAmount())
            );
        }

        double oldRemaining = contract.getRemainingAmount();
        double oldTotalPaid = contract.getTotalPaid();

        // Créer le paiement
        Payment payment = new Payment();
        payment.setAmount(amount);

        // Gérer les différentes méthodes de paiement
        try {
            if ("CARD".equalsIgnoreCase(paymentMethod) || "CREDIT_CARD".equalsIgnoreCase(paymentMethod)) {
                payment.setPaymentMethod(PaymentMethod.CARD);
            } else if ("CASH".equalsIgnoreCase(paymentMethod)) {
                payment.setPaymentMethod(PaymentMethod.CASH);
            } else if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethod)) {
                payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
            } else {
                payment.setPaymentMethod(PaymentMethod.valueOf(paymentMethod.toUpperCase()));
            }
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Méthode de paiement inconnue: {}, utilisation de CARD par défaut", paymentMethod);
            payment.setPaymentMethod(PaymentMethod.CARD);
        }

        payment.setPaymentDate(new Date());
        payment.setStatus(PaymentStatus.PAID);
        payment.setContract(contract);

        // Mettre à jour le contrat
        contract.setRemainingAmount(oldRemaining - amount);
        contract.setTotalPaid(oldTotalPaid + amount);

        // Sauvegarder
        Payment savedPayment = paymentRepository.save(payment);
        contractRepository.save(contract);

        log.info("💰 Paiement interne créé: {} DT | Reste: {} -> {} DT | Total payé: {} -> {} DT | Méthode: {}",
                amount, oldRemaining, contract.getRemainingAmount(),
                oldTotalPaid, contract.getTotalPaid(), payment.getPaymentMethod());

        // Vérifier si contrat terminé
        checkAndMarkContractAsCompletedAfterPayment(contract);

        return savedPayment;
    }

    /**
     * Vérifier et marquer le contrat comme COMPLETED si tous les paiements sont effectués
     */
    private void checkAndMarkContractAsCompletedAfterPayment(InsuranceContract contract) {
        Date today = new Date();
        Date endDate = contract.getEndDate();

        if (endDate != null && endDate.before(today)) {
            boolean allPaymentsPaid = contract.getPayments().stream()
                    .allMatch(p -> p.getStatus() == PaymentStatus.PAID);

            double totalPaid = contract.getTotalPaid();
            double premium = contract.getPremium();
            boolean isFullyPaid = Math.abs(totalPaid - premium) < 0.01;

            if (allPaymentsPaid && isFullyPaid) {
                if (contract.getStatus() != ContractStatus.COMPLETED) {
                    ContractStatus oldStatus = contract.getStatus();
                    contract.setStatus(ContractStatus.COMPLETED);
                    contractRepository.save(contract);

                    log.info("🎉 Contrat {} marqué COMPLETED après paiement", contract.getContractId());
                    log.info("   - Total payé: {} DT", totalPaid);
                    log.info("   - Prime totale: {} DT", premium);
                    log.info("   - Ancien statut: {}", oldStatus);

                    try {
                        emailService.sendContractCompletedEmail(contract.getClient(), contract);
                        log.info("📧 Email de contrat complété envoyé à {}", contract.getClient().getEmail());
                    } catch (Exception e) {
                        log.error("❌ Erreur envoi email contrat complété: {}", e.getMessage());
                    }
                }
            } else if (Math.abs(totalPaid - premium) < 0.01 && !allPaymentsPaid) {
                log.warn("⚠️ Contrat {}: total payé ({}) = prime ({}) mais certains paiements ne sont pas PAID",
                        contract.getContractId(), totalPaid, premium);
            }
        }
    }

    // ==================== MÉTHODES DE RAPPEL ====================

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkUpcomingPayments() {
        log.info("📧 Début de la vérification des paiements à venir - {}", new Date());

        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        Date today = new Date();

        int remindersSent = 0;

        for (InsuranceContract contract : activeContracts) {
            remindersSent += checkAndSendRemindersForContract(contract, today);
        }

        log.info("📧 Vérification terminée. {} rappels envoyés.", remindersSent);
    }

    private int checkAndSendRemindersForContract(InsuranceContract contract, Date today) {
        if (contract.getPayments() == null || contract.getPayments().isEmpty()) {
            return 0;
        }

        int remindersSent = 0;

        for (Payment payment : contract.getPayments()) {
            if (payment.getStatus() != PaymentStatus.PENDING) continue;

            Date paymentDate = payment.getPaymentDate();
            if (paymentDate == null) {
                log.warn("⚠️ Paiement {} sans date pour le contrat {}",
                        payment.getPaymentId(), contract.getContractId());
                continue;
            }

            long diffInDays = TimeUnit.DAYS.convert(
                    paymentDate.getTime() - today.getTime(), TimeUnit.MILLISECONDS);

            int[] reminderDays = {30, 15, 7, 3, 1};

            for (int daysBefore : reminderDays) {
                if (diffInDays == daysBefore) {
                    try {
                        emailService.sendPaymentReminderEmail(
                                contract.getClient(),
                                contract,
                                payment,
                                daysBefore
                        );
                        remindersSent++;
                        log.info("📧 Rappel J-{} envoyé pour contrat {} (paiement {})",
                                daysBefore, contract.getContractId(), payment.getPaymentId());
                    } catch (Exception e) {
                        log.error("❌ Erreur envoi rappel pour contrat {}: {}",
                                contract.getContractId(), e.getMessage());
                    }
                }
            }
        }

        return remindersSent;
    }

    public int checkAndSendRemindersForContract(Long contractId) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));
        return checkAndSendRemindersForContract(contract, new Date());
    }

    // ==================== MÉTHODES NON SUPPORTÉES ====================

    @Override
    public PaymentDTO updatePayment(PaymentDTO dto) {
        throw new UnsupportedOperationException("Modification de paiement non autorisée");
    }

    @Override
    public void deletePayment(Long id) {
        throw new UnsupportedOperationException("Suppression de paiement non autorisée");
    }

    // ==================== MÉTHODES UTILITAIRES PRIVÉES ====================

    private Date calculateDueDate(Date startDate, int installmentNumber) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.MONTH, installmentNumber);
        return cal.getTime();
    }

    /**
     * Traiter un paiement manuel (CASH ou BANK_TRANSFER)
     */
    @Transactional
    public PaymentDTO processManualPayment(Long contractId, double amount, String paymentMethod, String userEmail) {
        // 1. Récupérer le contrat
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        // 2. Vérifier les droits
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user instanceof Client && !contract.getClient().getId().equals(user.getId())) {
            throw new AccessDeniedException("Vous ne pouvez payer que vos propres contrats");
        }

        // 3. Vérifier le statut du contrat
        if (contract.getStatus() == ContractStatus.COMPLETED) {
            throw new RuntimeException("Ce contrat est déjà complété");
        }

        // 4. Récupérer la première tranche non payée
        Payment pendingPayment = contract.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .min(Comparator.comparing(Payment::getPaymentDate))
                .orElseThrow(() -> new RuntimeException("Aucune tranche à payer"));

        // 5. Vérifier le montant
        if (Math.abs(amount - pendingPayment.getAmount()) > 0.01) {
            throw new RuntimeException("Le montant ne correspond pas à la tranche due");
        }

        // 6. Marquer le paiement comme payé
        pendingPayment.setStatus(PaymentStatus.PAID);
        pendingPayment.setPaymentMethod(PaymentMethod.valueOf(paymentMethod));
        pendingPayment.setPaymentDate(new Date());
        paymentRepository.save(pendingPayment);

        // 7. Mettre à jour le contrat
        contract.setTotalPaid(contract.getTotalPaid() + amount);
        contract.setRemainingAmount(contract.getRemainingAmount() - amount);

        if (contract.getRemainingAmount() <= 0.01) {
            contract.setStatus(ContractStatus.COMPLETED);
            contract.setRemainingAmount(0.0);
        }

        contractRepository.save(contract);

        // 8. Envoyer email
        try {
            emailService.sendPaymentConfirmationEmail(contract.getClient(), contract, pendingPayment);
        } catch (Exception e) {
            log.error("Erreur envoi email: {}", e.getMessage());
        }

        log.info("✅ Paiement manuel de {} DT effectué pour le contrat {} via {}",
                amount, contractId, paymentMethod);

        return PaymentMapper.toDTO(pendingPayment);
    }
}