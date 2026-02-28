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
import java.util.List;
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
            // Un client ne peut payer que ses propres contrats
            if (!contract.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous ne pouvez payer que vos propres contrats");
            }
        } else if (user instanceof AgentAssurance) {
            // Un agent peut payer pour ses clients
            AgentAssurance agent = (AgentAssurance) user;
            if (!contract.getClient().getAgentAssurance().getId().equals(agent.getId())) {
                throw new AccessDeniedException("Ce contrat n'appartient pas à un de vos clients");
            }
        }
        // Admin peut tout faire

        // 4. Créer le paiement
        Payment payment = PaymentMapper.toEntity(dto);
        payment.setContract(contract);
        payment.setPaymentDate(new Date());
        payment.setStatus(PaymentStatus.PAID);

        // 5. Appliquer le paiement au contrat
        contract.applyPayment(payment.getAmount());

        // 6. Sauvegarder
        payment = paymentRepository.save(payment);
        contractRepository.save(contract);

        // 7. Vérifier si le contrat est maintenant COMPLETED
        checkAndMarkContractAsCompletedAfterPayment(contract);

        return PaymentMapper.toDTO(payment);
    }

    @Override
    public PaymentDTO getPaymentById(Long id, String userEmail) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        InsuranceContract contract = payment.getContract();

        // Vérification des droits
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
        // Admin peut tout voir

        return PaymentMapper.toDTO(payment);
    }

    @Override
    public List<PaymentDTO> getAllPayments(String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user instanceof Client) {
            return paymentRepository
                    .findByContract_ClientId(user.getId())
                    .stream()
                    .map(PaymentMapper::toDTO)
                    .toList();
        }

        if (user instanceof AgentAssurance agent) {
            return paymentRepository
                    .findByContract_AgentAssuranceId(agent.getId())
                    .stream()
                    .map(PaymentMapper::toDTO)
                    .toList();
        }

        // ADMIN
        return paymentRepository.findAll()
                .stream()
                .map(PaymentMapper::toDTO)
                .toList();
    }

    @Override
    public List<PaymentDTO> getPaymentsByContractId(Long contractId, String userEmail) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérification des droits
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
        // Admin peut tout voir

        return contract.getPayments().stream()
                .map(PaymentMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Méthodes Stripe (garder sans sécurité car ce sont des appels système)
    @Override
    public PaymentIntent createStripePaymentIntent(Long contractId) throws StripeException {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        long amountInCents = (long) (contract.getRemainingAmount() * 100);

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

    @Override
    public void handleSuccessfulPayment(String stripePaymentId, Long amountInCents) {
        double amountDT = amountInCents / 100.0;
        System.out.println("Paiement Stripe réussi : " + stripePaymentId + ", montant : " + amountDT);
        // Ici vous devriez retrouver le contrat via les metadata et appeler addPayment
    }

    // Méthodes non utilisées (garder pour compatibilité)
    @Override
    public PaymentDTO updatePayment(PaymentDTO dto) {
        throw new RuntimeException("Modification paiement interdite");
    }

    @Override
    public void deletePayment(Long id) {
        throw new RuntimeException("Suppression paiement interdite");
    }

    // Méthode privée pour vérifier si le contrat est complété
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


    /**
     * Vérifie les paiements à venir et envoie des rappels
     * S'exécute tous les jours à 8h00
     */
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
            // Ne vérifier que les paiements en attente
            if (payment.getStatus() != PaymentStatus.PENDING) continue;

            Date paymentDate = payment.getPaymentDate();
            long diffInMillies = paymentDate.getTime() - today.getTime();
            long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

            // Jours de rappel configurables
            int[] reminderDays = {30, 15, 7, 3, 1}; // Vous pouvez modifier ces valeurs

            for (int daysBefore : reminderDays) {
                if (diffInDays == daysBefore) {
                    emailService.sendPaymentReminderEmail(
                            contract.getClient(),
                            contract,
                            payment,
                            daysBefore
                    );
                    remindersSent++;

                    // Log pour le suivi
                    log.info("Rappel J-{} envoyé pour le contrat {} (paiement {})",
                            daysBefore, contract.getContractId(), payment.getPaymentId());
                }
            }
        }

        return remindersSent;
    }

    /**
     * Vérification manuelle pour un contrat spécifique
     */
    public int checkAndSendRemindersForContract(Long contractId) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        return checkAndSendRemindersForContract(contract, new Date());
    }
}