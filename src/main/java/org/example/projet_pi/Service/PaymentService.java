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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        // 4. Vérifier que le montant ne dépasse pas le reste à payer
        if (dto.getAmount() > contract.getRemainingAmount()) {
            throw new RuntimeException("Le montant du paiement (" + dto.getAmount() +
                    ") dépasse le reste à payer (" + contract.getRemainingAmount() + ")");
        }

        // 5. Créer le paiement
        Payment payment = PaymentMapper.toEntity(dto);
        payment.setContract(contract);
        payment.setPaymentDate(new Date());
        payment.setStatus(PaymentStatus.PAID);

        // 6. Appliquer le paiement au contrat
        contract.applyPayment(payment.getAmount());

        // 7. Sauvegarder
        payment = paymentRepository.save(payment);
        contractRepository.save(contract);

        // 8. Vérifier si le contrat est maintenant COMPLETED
        checkAndMarkContractAsCompletedAfterPayment(contract);

        log.info("✅ Paiement ajouté: {} DT pour le contrat {} (reste: {} DT)",
                payment.getAmount(), contract.getContractId(), contract.getRemainingAmount());

        return PaymentMapper.toDTO(payment);
    }

    // ==================== MÉTHODES STRIPE AVEC GESTION DES TRANCHES ====================

    @Override
    public PaymentIntent createStripePaymentIntent(Long contractId) throws StripeException {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

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

    /**
     * Créer un PaymentIntent pour un montant spécifique (tranche personnalisée)
     */
    public PaymentIntent createCustomStripePaymentIntent(Long contractId, Double amount) throws StripeException {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        if (amount > contract.getRemainingAmount()) {
            throw new RuntimeException("Le montant demandé (" + amount +
                    ") dépasse le reste à payer (" + contract.getRemainingAmount() + ")");
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
            log.info("🔄 Traitement du paiement Stripe: {} pour le contrat {}",
                    stripePaymentId, contractId);

            // 🔹 1. Récupérer le contrat
            InsuranceContract contract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé: " + contractId));

            double amountDT = amountInCents / 100.0;

            log.info("💰 Montant reçu: {} DT", amountDT);
            log.info("📊 Reste AVANT paiement: {}", contract.getRemainingAmount());

            // 🔹 2. Vérifier double traitement (sécurité Stripe)
            boolean alreadyProcessed = contract.getPayments().stream()
                    .anyMatch(p -> Math.abs(amountDT - p.getAmount()) < 0.01 &&
                            p.getPaymentDate().getTime() > System.currentTimeMillis() - 60000);

            if (alreadyProcessed) {
                log.warn("⚠️ Paiement déjà traité récemment pour le contrat {}", contractId);
                return;
            }

            // 🔹 3. Vérifier montant valide
            if (amountDT > contract.getRemainingAmount() + 0.01) {
                log.error("❌ Montant trop élevé: {} > reste à payer {}",
                        amountDT, contract.getRemainingAmount());
                throw new RuntimeException("Le montant du paiement dépasse le reste à payer");
            }

            // 🔹 4. Créer le DTO
            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setAmount(amountDT);
            paymentDTO.setPaymentMethod("CARD");
            paymentDTO.setContractId(contractId);

            // 🔹 5. Enregistrer le paiement (met à jour le contrat automatiquement)
            PaymentDTO savedPayment = addPayment(paymentDTO, contract.getClient().getEmail());

            // 🔹 6. Recharger le contrat pour vérifier mise à jour
            InsuranceContract updatedContract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé après paiement"));

            log.info("📊 Reste APRÈS paiement: {}", updatedContract.getRemainingAmount());

            log.info("✅ Paiement Stripe {} traité avec succès pour le contrat {}",
                    stripePaymentId, contractId);

            // 🔹 7. Email confirmation
            try {
                Payment payment = paymentRepository.findById(savedPayment.getPaymentId())
                        .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

                emailService.sendPaymentConfirmationEmail(
                        updatedContract.getClient(),
                        updatedContract,
                        payment
                );

                log.info("📧 Email de confirmation envoyé à {}",
                        updatedContract.getClient().getEmail());

            } catch (Exception e) {
                log.error("❌ Erreur envoi email confirmation: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du paiement Stripe: {}", e.getMessage());
            throw new RuntimeException("Erreur traitement paiement: " + e.getMessage());
        }
    }

    @Override
    public void handleSuccessfulPayment(String stripePaymentId, Long amountInCents) {
        log.warn("⚠️ Appel à la méthode de fallback handleSuccessfulPayment sans contractId");
        log.info("Paiement Stripe reçu: {} pour {} centimes", stripePaymentId, amountInCents);
    }

    // ==================== AUTRES MÉTHODES (inchangées) ====================

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

    @Override
    public PaymentDTO updatePayment(PaymentDTO dto) {
        throw new RuntimeException("Modification paiement interdite");
    }

    @Override
    public void deletePayment(Long id) {
        throw new RuntimeException("Suppression paiement interdite");
    }

    private void checkAndMarkContractAsCompletedAfterPayment(InsuranceContract contract) {
        Date today = new Date();
        Date endDate = contract.getEndDate();

        if (endDate != null && endDate.before(today)) {
            boolean allPaymentsPaid = contract.getPayments().stream()
                    .allMatch(p -> p.getStatus() == PaymentStatus.PAID);

            if (allPaymentsPaid && Math.abs(contract.getTotalPaid() - contract.getPremium()) < 0.01) {
                contract.setStatus(ContractStatus.COMPLETED);
                contractRepository.save(contract);
                log.info("🎉 Contrat {} marqué COMPLETED après paiement", contract.getContractId());
            }
        }
    }

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
            long diffInDays = TimeUnit.DAYS.convert(
                    paymentDate.getTime() - today.getTime(), TimeUnit.MILLISECONDS);

            int[] reminderDays = {30, 15, 7, 3, 1};

            for (int daysBefore : reminderDays) {
                if (diffInDays == daysBefore) {
                    emailService.sendPaymentReminderEmail(
                            contract.getClient(),
                            contract,
                            payment,
                            daysBefore
                    );
                    remindersSent++;
                    log.info("Rappel J-{} envoyé pour le contrat {} (paiement {})",
                            daysBefore, contract.getContractId(), payment.getPaymentId());
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
}