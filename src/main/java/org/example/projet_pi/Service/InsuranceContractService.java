package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Dto.InsuranceContractDTO;
import org.example.projet_pi.Mapper.InsuranceContractMapper;
import org.example.projet_pi.Repository.*;
import org.example.projet_pi.entity.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class InsuranceContractService implements IInsuranceContractService {

    private final InsuranceContractRepository contractRepository;
    private final ClientRepository clientRepository;
    private final AgentAssuranceRepository agentRepository;
    private final InsuranceProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ============================================================
    // 🔥 ADD CONTRACT
    // ============================================================

    @Override
    @Transactional
    public InsuranceContractDTO addContract(InsuranceContractDTO dto, String userEmail) {
        // 1. Récupérer le client connecté
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!(user instanceof Client)) {
            throw new AccessDeniedException("Seuls les clients peuvent créer des contrats");
        }

        Client client = (Client) user;

        // 2. Vérifier que le client a un agent d'assurance assigné
        if (client.getAgentAssurance() == null) {
            throw new RuntimeException("Vous devez avoir un agent d'assurance assigné pour créer un contrat");
        }

        // 3. Création du contrat
        InsuranceContract contract = InsuranceContractMapper.toEntity(dto);
        contract.setClient(client);
        contract.setAgentAssurance(client.getAgentAssurance());

        InsuranceProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        contract.setProduct(product);

        contract.setPaymentFrequency(Enum.valueOf(PaymentFrequency.class, dto.getPaymentFrequency()));
        contract.setTotalPaid(0.0);
        contract.setRemainingAmount(contract.getPremium());

        // 4. Calcul du risque
        RiskClaim riskClaim = calculateRisk(contract);
        riskClaim.setContract(contract);
        contract.setRiskClaim(riskClaim);

        // 5. Statut initial: INACTIF (en attente d'approbation par l'agent)
        if ("HIGH".equals(riskClaim.getRiskLevel())) {
            contract.setStatus(ContractStatus.CANCELLED);
        } else {
            contract.setStatus(ContractStatus.INACTIVE);
        }

        // 6. Génération des paiements planifiés
        contract.setPayments(new ArrayList<>());
        generateScheduledPayments(contract);

        contract = contractRepository.save(contract);
        return InsuranceContractMapper.toDTO(contract);
    }

    @Override
    @Transactional
    public InsuranceContractDTO activateContract(Long contractId, String agentEmail) {
        log.info("🔧 Tentative d'activation du contrat {} par l'agent {}", contractId, agentEmail);

        // 1. Vérifier que c'est bien un agent
        User user = userRepository.findByEmail(agentEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!(user instanceof AgentAssurance)) {
            throw new AccessDeniedException("Seuls les agents d'assurance peuvent activer des contrats");
        }

        AgentAssurance agent = (AgentAssurance) user;
        log.info("✅ Agent authentifié: {} {}", agent.getFirstName(), agent.getLastName());

        // 2. Récupérer le contrat
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        log.info("📄 Contrat trouvé: ID={}, Statut={}, Client={}",
                contract.getContractId(), contract.getStatus(),
                contract.getClient().getEmail());

        // 3. Vérifier que le contrat appartient bien à un client de cet agent
        if (!contract.getClient().getAgentAssurance().getId().equals(agent.getId())) {
            log.error("❌ Contrat {} n'appartient pas à l'agent {}", contractId, agent.getId());
            throw new AccessDeniedException("Ce contrat n'appartient pas à un de vos clients");
        }

        // 4. Vérifier que le contrat est INACTIVE
        if (contract.getStatus() != ContractStatus.INACTIVE) {
            log.error("❌ Contrat {} n'est pas INACTIVE (statut: {})", contractId, contract.getStatus());
            throw new RuntimeException("Seuls les contrats INACTIVE peuvent être activés");
        }

        // 5. Vérifier le niveau de risque
        if (contract.getRiskClaim() != null && "HIGH".equals(contract.getRiskClaim().getRiskLevel())) {
            log.error("❌ Contrat {} a un risque HIGH, activation impossible", contractId);
            throw new RuntimeException("Impossible d'activer un contrat à risque HIGH");
        }

        // 6. Activer le contrat
        contract.setStatus(ContractStatus.ACTIVE);
        contract = contractRepository.save(contract);
        log.info("✅ Contrat {} activé avec succès", contractId);

        // 7. Envoyer un email de confirmation au client
        Client client = contract.getClient();
        try {
            emailService.sendContractAcceptedEmail(client, contract);
            log.info("✅ Email de confirmation envoyé à {} pour le contrat {}",
                    client.getEmail(), contract.getContractId());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de confirmation: {}", e.getMessage());
            // Ne pas bloquer l'activation même si l'email échoue
        }

        return InsuranceContractMapper.toDTO(contract);
    }

    // 🔥 NOUVELLE MÉTHODE : Activer et envoyer email en une seule opération
    @Transactional
    public InsuranceContractDTO activateAndNotify(Long contractId, String agentEmail) {
        InsuranceContractDTO activatedContract = activateContract(contractId, agentEmail);
        // L'email est déjà envoyé dans activateContract
        return activatedContract;
    }

    @Override
    public List<InsuranceContractDTO> getAllContracts(String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // ================= CLIENT =================
        if (user instanceof Client client) {

            return contractRepository.findByClient(client)
                    .stream()
                    .map(InsuranceContractMapper::toDTO)
                    .collect(Collectors.toList());
        }

        // ================= AGENT =================
        if (user instanceof AgentAssurance agent) {

            return contractRepository.findByAgentAssuranceId(agent.getId())
                    .stream()
                    .map(InsuranceContractMapper::toDTO)
                    .collect(Collectors.toList());
        }

        // ================= ADMIN =================
        return contractRepository.findAll()
                .stream()
                .map(InsuranceContractMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public InsuranceContractDTO getContractById(Long id, String userEmail) {
        InsuranceContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérification des droits d'accès
        if (user instanceof Client) {
            // Un client ne voit que ses propres contrats
            if (!contract.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous n'avez pas accès à ce contrat");
            }
        } else if (user instanceof AgentAssurance) {
            // Un agent ne voit que les contrats de ses clients
            AgentAssurance agent = (AgentAssurance) user;
            if (!contract.getClient().getAgentAssurance().getId().equals(agent.getId())) {
                throw new AccessDeniedException("Ce contrat n'appartient pas à un de vos clients");
            }
        }
        // Admin peut tout voir

        return InsuranceContractMapper.toDTO(contract);
    }

    @Override
    @Transactional
    public InsuranceContractDTO updateContract(InsuranceContractDTO dto, String userEmail) {
        InsuranceContract contract = contractRepository.findById(dto.getContractId())
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérification: seul le propriétaire ou admin peut modifier
        if (user instanceof Client) {
            if (!contract.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous ne pouvez modifier que vos propres contrats");
            }
        } else if (!(user instanceof Admin) && !(user instanceof AgentAssurance)) {
            throw new AccessDeniedException("Modification non autorisée");
        }

        // ======== MISE À JOUR DES CHAMPS DE BASE ========
        if (dto.getStartDate() != null) contract.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) contract.setEndDate(dto.getEndDate());
        if (dto.getPremium() > 0) contract.setPremium(dto.getPremium());
        if (dto.getDeductible() > 0) contract.setDeductible(dto.getDeductible());
        if (dto.getCoverageLimit() > 0) contract.setCoverageLimit(dto.getCoverageLimit());

        // 🔥 MISE À JOUR DU MONTANT RESTANT
        contract.setRemainingAmount(contract.getPremium() - contract.getTotalPaid());

        // 🔥 MISE À JOUR DU STATUT AUTOMATIQUE
        if (contract.getRemainingAmount() <= 0) {
            contract.setStatus(ContractStatus.COMPLETED);
        } else if (dto.getStatus() != null) {
            contract.setStatus(Enum.valueOf(ContractStatus.class, dto.getStatus()));
        }

        // ======== MISE À JOUR DE LA FREQUENCE DE PAIEMENT ========
        if (dto.getPaymentFrequency() != null) {
            PaymentFrequency newFrequency = Enum.valueOf(PaymentFrequency.class, dto.getPaymentFrequency());

            if (contract.getPaymentFrequency() != newFrequency) {
                contract.setPaymentFrequency(newFrequency);
                regenerateScheduledPayments(contract);
            }
        }

        // ======== MISE À JOUR DES LIENS ========
        if (dto.getClientId() != null) {
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            contract.setClient(client);
        }

        if (dto.getAgentAssuranceId() != null) {
            AgentAssurance agent = agentRepository.findById(dto.getAgentAssuranceId())
                    .orElseThrow(() -> new RuntimeException("Agent not found"));
            contract.setAgentAssurance(agent);
        }

        if (dto.getProductId() != null) {
            InsuranceProduct product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            contract.setProduct(product);
        }

        // ======== RE-CALCUL DU RISQUE ========
        RiskClaim existingRisk = contract.getRiskClaim();
        if (existingRisk == null) {
            existingRisk = calculateRisk(contract);
            existingRisk.setContract(contract);
            contract.setRiskClaim(existingRisk);
        } else {
            RiskClaim updatedRisk = calculateRisk(contract);
            existingRisk.setRiskScore(updatedRisk.getRiskScore());
            existingRisk.setRiskLevel(updatedRisk.getRiskLevel());
            existingRisk.setEvaluationNote(updatedRisk.getEvaluationNote());
        }

        // Si risque HIGH, annuler automatiquement
        if ("HIGH".equals(contract.getRiskClaim().getRiskLevel())) {
            contract.setStatus(ContractStatus.CANCELLED);
        }

        contract = contractRepository.save(contract);
        return InsuranceContractMapper.toDTO(contract);
    }

    @Override
    @Transactional
    public void deleteContract(Long id, String userEmail) {
        InsuranceContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérification: seul le propriétaire ou admin peut supprimer
        if (user instanceof Client) {
            if (!contract.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres contrats");
            }
        } else if (!(user instanceof Admin)) {
            throw new AccessDeniedException("Suppression non autorisée");
        }

        contractRepository.deleteById(id);
    }

    // ============================================================
    // 🔥 GÉNÉRATION DES PAIEMENTS
    // ============================================================

    private void generateScheduledPayments(InsuranceContract contract) {
        if (contract.getPaymentFrequency() == null) return;

        Date start = contract.getStartDate();
        Date end = contract.getEndDate();

        if (start == null || end == null) return;

        double installment = contract.calculateInstallmentAmount();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);

        while (!calendar.getTime().after(end)) {
            Payment payment = new Payment();
            payment.setContract(contract);
            payment.setAmount(installment);
            payment.setPaymentDate(calendar.getTime());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

            contract.getPayments().add(payment);

            switch (contract.getPaymentFrequency()) {
                case MONTHLY -> calendar.add(Calendar.MONTH, 1);
                case SEMI_ANNUAL -> calendar.add(Calendar.MONTH, 6);
                case ANNUAL -> calendar.add(Calendar.YEAR, 1);
            }
        }
    }

    private void regenerateScheduledPayments(InsuranceContract contract) {
        if (contract.getPayments() == null) {
            contract.setPayments(new ArrayList<>());
            return;
        }

        contract.getPayments().removeIf(p -> p.getStatus() == PaymentStatus.PENDING);
        generateScheduledPayments(contract);
    }

    // ============================================================
// 🔥 CALCUL DU RISQUE - VERSION AMÉLIORÉE
// ============================================================

    private RiskClaim calculateRisk(InsuranceContract contract) {
        RiskClaim riskClaim = new RiskClaim();
        riskClaim.setContract(contract);

        double score = 0;
        StringBuilder evaluation = new StringBuilder();

        // 1. Évaluation de la prime
        if (contract.getPremium() > 10000) {
            score += 40;
            evaluation.append("Prime très élevée (>10000 DT). ");
        } else if (contract.getPremium() > 5000) {
            score += 25;
            evaluation.append("Prime élevée (>5000 DT). ");
        } else {
            score += 10;
            evaluation.append("Prime normale. ");
        }

        // 2. Évaluation de la franchise
        if (contract.getDeductible() < 200) {
            score += 30;
            evaluation.append("Franchise très basse (<200 DT). ");
        } else if (contract.getDeductible() < 500) {
            score += 20;
            evaluation.append("Franchise basse. ");
        } else {
            score += 10;
            evaluation.append("Franchise acceptable. ");
        }

        // 3. Évaluation du plafond
        if (contract.getCoverageLimit() > 100000) {
            score += 40;
            evaluation.append("Plafond très élevé (>100000 DT). ");
        } else if (contract.getCoverageLimit() > 50000) {
            score += 30;
            evaluation.append("Plafond élevé (>50000 DT). ");
        } else if (contract.getCoverageLimit() > 20000) {
            score += 20;
            evaluation.append("Plafond modéré. ");
        } else {
            score += 10;
            evaluation.append("Plafond normal. ");
        }

        // 4. Évaluation de la durée
        if (contract.getStartDate() != null && contract.getEndDate() != null) {
            long durationInDays = (contract.getEndDate().getTime() - contract.getStartDate().getTime()) / (1000 * 60 * 60 * 24);
            if (durationInDays > 365 * 3) { // plus de 3 ans
                score += 20;
                evaluation.append("Durée longue (>3 ans). ");
            } else if (durationInDays > 365) { // plus d'un an
                score += 10;
                evaluation.append("Durée moyenne. ");
            } else {
                score += 5;
                evaluation.append("Durée courte. ");
            }
        }

        riskClaim.setRiskScore(score);

        // Déterminer le niveau de risque
        if (score >= 80) {
            riskClaim.setRiskLevel("HIGH");
            evaluation.insert(0, "🔴 RISQUE ÉLEVÉ - ");
            riskClaim.setEvaluationNote(evaluation.toString());
        } else if (score >= 50) {
            riskClaim.setRiskLevel("MEDIUM");
            evaluation.insert(0, "🟡 RISQUE MOYEN - ");
            riskClaim.setEvaluationNote(evaluation.toString());
        } else {
            riskClaim.setRiskLevel("LOW");
            evaluation.insert(0, "🟢 RISQUE FAIBLE - ");
            riskClaim.setEvaluationNote(evaluation.toString());
        }

        return riskClaim;
    }

    // ============================================================
    // 🔥 VÉRIFICATION DES RETARDS DE PAIEMENT
    // ============================================================

    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void checkLatePayments() {
        log.info("🔍 Vérification quotidienne des retards - {}", new Date());

        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        log.info("📊 {} contrat(s) actif(s)", activeContracts.size());

        int contractsCancelled = 0;
        int totalLatePayments = 0;

        for (InsuranceContract contract : activeContracts) {
            // Marquer les nouveaux retards
            checkAndMarkLatePaymentsByFrequency(contract);

            // Compter les retards avant annulation
            int beforeLateCount = countLatePayments(contract);

            // Vérifier si le contrat doit être annulé (≥ 4 retards)
            ContractStatus beforeStatus = contract.getStatus();
            checkAndCancelContractForLatePayments(contract);
            ContractStatus afterStatus = contract.getStatus();

            // Compter les retards après traitement
            int afterLateCount = countLatePayments(contract);

            if (beforeStatus != afterStatus && afterStatus == ContractStatus.CANCELLED) {
                contractsCancelled++;
                log.warn("🚨 Contrat {} annulé (avait {} retards)", contract.getContractId(), afterLateCount);
            }

            totalLatePayments += afterLateCount;
        }

        // Vérifier aussi les contrats INACTIVE
        List<InsuranceContract> inactiveContracts = contractRepository.findByStatus(ContractStatus.INACTIVE);
        for (InsuranceContract contract : inactiveContracts) {
            checkAndMarkLatePaymentsByFrequency(contract);
        }

        log.info("📊 Résumé: {} contrat(s) annulé(s), {} total paiements en retard",
                contractsCancelled, totalLatePayments);
    }

    /**
     * Compter le nombre de paiements en retard pour un contrat
     */
    private int countLatePayments(InsuranceContract contract) {
        if (contract.getPayments() == null) return 0;
        return (int) contract.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.LATE)
                .count();
    }

    @Override
    @Scheduled(cron = "0 59 23 L * ?")
    @Transactional
    public void checkEndOfMonthLatePayments() {
        System.out.println("📅 Vérification de fin de mois - " + new Date());

        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        for (InsuranceContract contract : activeContracts) {
            checkAndMarkMonthlyLatePayments(contract);
            checkAndCancelContractForLatePayments(contract);
        }

        List<InsuranceContract> inactiveContracts = contractRepository.findByStatus(ContractStatus.INACTIVE);
        for (InsuranceContract contract : inactiveContracts) {
            checkAndMarkMonthlyLatePayments(contract);
        }
    }

    @Override
    public void checkContractLatePayments(Long contractId) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        System.out.println("🔍 Vérification manuelle du contrat " + contractId);
        checkAndMarkLatePaymentsByFrequency(contract);
        checkAndCancelContractForLatePayments(contract);
    }

    @Override
    public void simulateLatePayments(Long contractId, int monthsToAdd) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        Calendar cal = Calendar.getInstance();
        for (Payment payment : contract.getPayments()) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                cal.setTime(payment.getPaymentDate());
                cal.add(Calendar.MONTH, -monthsToAdd);
                payment.setPaymentDate(cal.getTime());
            }
        }

        contractRepository.save(contract);
        System.out.println("⏱️ Simulation: " + monthsToAdd + " mois de retard - contrat " + contractId);
    }

    @Override
    @Scheduled(cron = "0 30 0 * * ?")
    @Transactional
    public void checkCompletedContracts() {
        System.out.println("✅ Vérification des contrats à marquer COMPLETED - " + new Date());

        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        for (InsuranceContract contract : activeContracts) {
            checkAndMarkContractAsCompleted(contract);
        }

        List<InsuranceContract> inactiveContracts = contractRepository.findByStatus(ContractStatus.INACTIVE);
        for (InsuranceContract contract : inactiveContracts) {
            checkAndMarkContractAsCompleted(contract);
        }
    }

    // ============================================================
    // 🔥 MÉTHODES PRIVÉES
    // ============================================================

    private void checkAndMarkLatePaymentsByFrequency(InsuranceContract contract) {
        if (contract.getPayments() == null || contract.getPayments().isEmpty()) return;

        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        boolean paymentsUpdated = false;

        for (Payment payment : contract.getPayments()) {
            if (payment.getStatus() != PaymentStatus.PENDING) continue;

            Date paymentDate = payment.getPaymentDate();
            cal.setTime(paymentDate);

            switch (contract.getPaymentFrequency()) {
                case MONTHLY:
                    cal.add(Calendar.MONTH, 1);
                    break;
                case SEMI_ANNUAL:
                    cal.add(Calendar.MONTH, 6);
                    break;
                case ANNUAL:
                    cal.add(Calendar.YEAR, 1);
                    break;
            }

            if (cal.getTime().before(today)) {
                payment.setStatus(PaymentStatus.LATE);
                paymentsUpdated = true;
                System.out.println("⏰ Paiement " + payment.getPaymentId() + " marqué LATE");
            }
        }

        if (paymentsUpdated) contractRepository.save(contract);
    }

    private void checkAndMarkMonthlyLatePayments(InsuranceContract contract) {
        if (contract.getPayments() == null || contract.getPayments().isEmpty()) return;

        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date firstDayOfMonth = cal.getTime();

        boolean paymentsUpdated = false;

        for (Payment payment : contract.getPayments()) {
            if (payment.getStatus() == PaymentStatus.PENDING &&
                    payment.getPaymentDate().before(firstDayOfMonth) &&
                    contract.getPaymentFrequency() == PaymentFrequency.MONTHLY) {

                payment.setStatus(PaymentStatus.LATE);
                paymentsUpdated = true;
                System.out.println("📆 Paiement mensuel " + payment.getPaymentId() + " marqué LATE");
            }
        }

        if (paymentsUpdated) contractRepository.save(contract);
    }

    private void checkAndCancelContractForLatePayments(InsuranceContract contract) {
        if (contract.getPayments() == null || contract.getPayments().isEmpty()) return;

        int latePaymentCount = 0;
        int pendingPaymentCount = 0;
        boolean paymentsUpdated = false;

        // Compter les paiements en retard et en attente
        for (Payment payment : contract.getPayments()) {
            if (payment.getStatus() == PaymentStatus.LATE) {
                latePaymentCount++;
            } else if (payment.getStatus() == PaymentStatus.PENDING) {
                pendingPaymentCount++;
            }
        }

        // 🚨 NOUVEAU SEUIL: 4 paiements en retard pour annulation
        if (latePaymentCount >= 4) {
            log.warn("🚨 CONTRAT {} ANNULÉ - {} paiements en retard (seuil: 4)",
                    contract.getContractId(), latePaymentCount);

            // Changer le statut du contrat
            contract.setStatus(ContractStatus.CANCELLED);

            // Marquer tous les paiements en attente comme FAILED
            for (Payment payment : contract.getPayments()) {
                if (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.LATE) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentsUpdated = true;
                }
            }

            // 📧 Envoyer un email d'annulation au client
            Client client = contract.getClient();
            if (client != null && client.getEmail() != null) {
                try {
                    emailService.sendContractCancelledEmail(client, contract, latePaymentCount);
                    log.info("📧 Email d'annulation envoyé à {} pour le contrat {}",
                            client.getEmail(), contract.getContractId());
                } catch (Exception e) {
                    log.error("❌ Erreur lors de l'envoi de l'email d'annulation: {}", e.getMessage());
                }
            }

            log.info("📊 Statistiques - Contrat {}: {} paiements en retard, {} paiements en attente marqués FAILED",
                    contract.getContractId(), latePaymentCount, pendingPaymentCount);

        } else if (latePaymentCount > 0) {
            // Simple information si des retards existent mais pas encore 4
            log.info("ℹ️ Contrat {}: {} paiements en retard (seuil non atteint)",
                    contract.getContractId(), latePaymentCount);
        }

        if (paymentsUpdated || latePaymentCount >= 4) {
            contractRepository.save(contract);
        }
    }

    private void checkAndMarkContractAsCompleted(InsuranceContract contract) {
        if (contract.getPayments() == null || contract.getPayments().isEmpty()) {
            return;
        }

        Date today = new Date();
        Date endDate = contract.getEndDate();

        boolean isEndDatePassed = endDate != null && endDate.before(today);

        if (!isEndDatePassed) {
            return;
        }

        boolean allPaymentsPaid = true;
        int totalPayments = 0;
        int paidPayments = 0;

        for (Payment payment : contract.getPayments()) {
            totalPayments++;
            if (payment.getStatus() == PaymentStatus.PAID) {
                paidPayments++;
            } else {
                allPaymentsPaid = false;
            }
        }

        boolean totalPaidMatches = Math.abs(contract.getTotalPaid() - contract.getPremium()) < 0.01;

        if (allPaymentsPaid && totalPaidMatches) {
            contract.setStatus(ContractStatus.COMPLETED);
            contractRepository.save(contract);
            System.out.println("🎉 CONTRAT " + contract.getContractId() + " MARQUÉ COMPLETED");
        }
    }

    public List<InsuranceContractDTO> getContractsByClientEmail(String email) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        return contractRepository.findByClient(client)
                .stream()
                .map(InsuranceContractMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public InsuranceContractDTO rejectContract(Long contractId, String agentEmail, String rejectionReason) {
        // 1. Vérifier que c'est bien un agent
        User user = userRepository.findByEmail(agentEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!(user instanceof AgentAssurance)) {
            throw new AccessDeniedException("Seuls les agents d'assurance peuvent rejeter des contrats");
        }

        AgentAssurance agent = (AgentAssurance) user;

        // 2. Récupérer le contrat
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        // 3. Vérifier que le contrat appartient bien à un client de cet agent
        if (!contract.getClient().getAgentAssurance().getId().equals(agent.getId())) {
            throw new AccessDeniedException("Ce contrat n'appartient pas à un de vos clients");
        }

        // 4. Vérifier que le contrat est INACTIVE
        if (contract.getStatus() != ContractStatus.INACTIVE) {
            throw new RuntimeException("Seuls les contrats INACTIVE peuvent être rejetés");
        }

        // 5. Rejeter le contrat en utilisant CANCELLED
        contract.setStatus(ContractStatus.CANCELLED);
        contract = contractRepository.save(contract);

        // 📧 6. Envoyer un email de notification au client
        Client client = contract.getClient();
        try {
            emailService.sendContractRejectedEmail(client, contract, rejectionReason);
            log.info("✅ Email de rejet envoyé à {} pour le contrat {}",
                    client.getEmail(), contract.getContractId());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de rejet: {}", e.getMessage());
            // Ne pas bloquer le rejet même si l'email échoue
        }

        // Log pour traçabilité
        log.info("❌ Contrat {} rejeté par l'agent {} - Raison: {}",
                contractId, agentEmail, rejectionReason);

        return InsuranceContractMapper.toDTO(contract);
    }

}