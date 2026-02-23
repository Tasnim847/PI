package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.InsuranceContractDTO;
import org.example.projet_pi.Mapper.InsuranceContractMapper;
import org.example.projet_pi.Repository.*;
import org.example.projet_pi.entity.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class InsuranceContractService implements IInsuranceContractService {

    private final InsuranceContractRepository contractRepository;
    private final ClientRepository clientRepository;
    private final AgentAssuranceRepository agentRepository;
    private final InsuranceProductRepository productRepository;

    // ============================================================
    // 🔥 ADD CONTRACT
    // ============================================================

    @Override
    public InsuranceContractDTO addContract(InsuranceContractDTO dto) {

        InsuranceContract contract = InsuranceContractMapper.toEntity(dto);

        if (contract.getStartDate() == null || contract.getEndDate() == null) {
            throw new RuntimeException("Dates obligatoires");
        }

        if (contract.getPremium() <= 0) {
            throw new RuntimeException("Prime invalide");
        }

        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        AgentAssurance agent = agentRepository.findById(dto.getAgentAssuranceId())
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        InsuranceProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        contract.setClient(client);
        contract.setAgentAssurance(agent);
        contract.setProduct(product);

        contract.setPaymentFrequency(
                Enum.valueOf(PaymentFrequency.class, dto.getPaymentFrequency())
        );

        // 🔥 INITIALISATION DES MONTANTS
        contract.setTotalPaid(0);
        contract.setRemainingAmount(contract.getPremium());

        // 🔥 CALCUL DU RISQUE
        RiskClaim riskClaim = calculateRisk(contract);
        riskClaim.setContract(contract);
        contract.setRiskClaim(riskClaim);

        // MODIFICATION 1: Le contrat est INACTIF par défaut, sauf si risque HIGH
        if ("HIGH".equals(riskClaim.getRiskLevel())) {
            contract.setStatus(ContractStatus.CANCELLED);
        } else {
            contract.setStatus(ContractStatus.INACTIVE);  // Changé de ACTIVE à INACTIVE
        }

        // 🔥 GÉNÉRATION DES PAIEMENTS PLANIFIÉS
        contract.setPayments(new ArrayList<>());
        generateScheduledPayments(contract);

        contract = contractRepository.save(contract);

        return InsuranceContractMapper.toDTO(contract);
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

    // ============================================================
    // 🔥 UPDATE CONTRACT
    // ============================================================

    @Override
    public InsuranceContractDTO updateContract(InsuranceContractDTO dto) {

        InsuranceContract contract = contractRepository.findById(dto.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

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
                regenerateScheduledPayments(contract); // ne supprime que les paiements PENDING
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

        // ======== RE-CALCUL DU RISQUE ET MISE À JOUR DU RISKCLAIM EXISTANT ========
        RiskClaim existingRisk = contract.getRiskClaim();
        if (existingRisk == null) {
            // Pas de RiskClaim existant → on en crée un nouveau
            existingRisk = calculateRisk(contract);
            existingRisk.setContract(contract);
            contract.setRiskClaim(existingRisk);
        } else {
            // RiskClaim existant → mise à jour des champs
            RiskClaim updatedRisk = calculateRisk(contract);
            existingRisk.setRiskScore(updatedRisk.getRiskScore());
            existingRisk.setRiskLevel(updatedRisk.getRiskLevel());
            existingRisk.setEvaluationNote(updatedRisk.getEvaluationNote());
        }

        // Si risque HIGH, annuler automatiquement
        if ("HIGH".equals(contract.getRiskClaim().getRiskLevel())) {
            contract.setStatus(ContractStatus.CANCELLED);
        }

        // ======== SAUVEGARDE ========
        contract = contractRepository.save(contract);

        return InsuranceContractMapper.toDTO(contract);
    }

    // ============================================================
    // 🔥 REGENERATION DES PAIEMENTS
    // ============================================================

    private void regenerateScheduledPayments(InsuranceContract contract) {

        if (contract.getPayments() == null) {
            contract.setPayments(new ArrayList<>());
            return;
        }

        // Supprimer seulement les paiements non encore payés
        contract.getPayments().removeIf(
                p -> p.getStatus() == PaymentStatus.PENDING
        );

        generateScheduledPayments(contract);
    }

    // ============================================================
    // CRUD
    // ============================================================

    @Override
    public void deleteContract(Long id) {
        contractRepository.deleteById(id);
    }

    @Override
    public InsuranceContractDTO getContractById(Long id) {
        InsuranceContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found"));
        return InsuranceContractMapper.toDTO(contract);
    }

    @Override
    public List<InsuranceContractDTO> getAllContracts() {
        return contractRepository.findAll()
                .stream()
                .map(InsuranceContractMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // 🔥 CALCUL DU RISQUE
    // ============================================================

    private RiskClaim calculateRisk(InsuranceContract contract) {

        RiskClaim riskClaim = new RiskClaim();
        riskClaim.setContract(contract);

        double score = 0;

        if (contract.getPremium() > 10000) score += 40;
        else if (contract.getPremium() > 5000) score += 25;
        else score += 10;

        if (contract.getDeductible() < 200) score += 30;
        else score += 10;

        if (contract.getCoverageLimit() > 50000) score += 30;
        else score += 10;

        riskClaim.setRiskScore(score);

        if (score >= 80) {
            riskClaim.setRiskLevel("HIGH");
            riskClaim.setEvaluationNote("Contrat à haut risque");
        } else if (score >= 50) {
            riskClaim.setRiskLevel("MEDIUM");
            riskClaim.setEvaluationNote("Contrat à risque modéré");
        } else {
            riskClaim.setRiskLevel("LOW");
            riskClaim.setEvaluationNote("Contrat à faible risque");
        }

        return riskClaim;
    }



// ============================================================
// 🔥 VÉRIFICATION DES RETARDS DE PAIEMENT - VERSION OPTIMISÉE
// ============================================================

    /**
     * Vérification quotidienne à minuit
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void checkLatePayments() {
        System.out.println("🔍 Vérification quotidienne des retards - " + new Date());

        // Contrats ACTIFS
        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        System.out.println("📊 " + activeContracts.size() + " contrat(s) actif(s)");

        for (InsuranceContract contract : activeContracts) {
            checkAndMarkLatePaymentsByFrequency(contract);
            checkAndCancelContractForLatePayments(contract);
        }

        // Contrats INACTIFS (mise à jour des statuts seulement)
        List<InsuranceContract> inactiveContracts = contractRepository.findByStatus(ContractStatus.INACTIVE);
        for (InsuranceContract contract : inactiveContracts) {
            checkAndMarkLatePaymentsByFrequency(contract);
        }
    }

    /**
     * Vérification de fin de mois à 23:59
     */
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

    /**
     * Marque les paiements en retard selon la fréquence
     */
    private void checkAndMarkLatePaymentsByFrequency(InsuranceContract contract) {
        if (contract.getPayments() == null || contract.getPayments().isEmpty()) return;

        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        boolean paymentsUpdated = false;

        for (Payment payment : contract.getPayments()) {
            if (payment.getStatus() != PaymentStatus.PENDING) continue;

            Date paymentDate = payment.getPaymentDate();
            cal.setTime(paymentDate);

            String period = "";
            switch (contract.getPaymentFrequency()) {
                case MONTHLY:
                    cal.add(Calendar.MONTH, 1);
                    period = "mois";
                    break;
                case SEMI_ANNUAL:
                    cal.add(Calendar.MONTH, 6);
                    period = "semestre";
                    break;
                case ANNUAL:
                    cal.add(Calendar.YEAR, 1);
                    period = "an";
                    break;
            }

            if (cal.getTime().before(today)) {
                payment.setStatus(PaymentStatus.LATE);
                paymentsUpdated = true;
                System.out.println("⏰ Paiement " + payment.getPaymentId() +
                        " (" + period + ") du " + paymentDate + " marqué LATE");
            }
        }

        if (paymentsUpdated) contractRepository.save(contract);
    }

    /**
     * Marque les paiements mensuels en retard (fin de mois)
     */
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
                System.out.println("📆 Paiement mensuel " + payment.getPaymentId() +
                        " du " + payment.getPaymentDate() + " marqué LATE");
            }
        }

        if (paymentsUpdated) contractRepository.save(contract);
    }

    /**
     * Vérifie et annule un contrat avec des retards
     */
    private void checkAndCancelContractForLatePayments(InsuranceContract contract) {
        if (contract.getPayments() == null || contract.getPayments().isEmpty()) return;

        int latePaymentCount = 0;
        boolean paymentsUpdated = false;

        for (Payment payment : contract.getPayments()) {
            if (payment.getStatus() == PaymentStatus.LATE) {
                latePaymentCount++;
            }
        }

        if (latePaymentCount >= 1) {
            System.out.println("🚨 CONTRAT " + contract.getContractId() +
                    " ANNULÉ - " + latePaymentCount + " retard(s)");

            contract.setStatus(ContractStatus.CANCELLED);

            for (Payment payment : contract.getPayments()) {
                if (payment.getStatus() == PaymentStatus.PENDING ||
                        payment.getStatus() == PaymentStatus.LATE) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentsUpdated = true;
                }
            }
        }

        if (paymentsUpdated || latePaymentCount >= 1) {
            contractRepository.save(contract);
        }
    }

    /**
     * Vérification manuelle d'un contrat
     */
    public void checkContractLatePayments(Long contractId) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        System.out.println("🔍 Vérification manuelle du contrat " + contractId);
        checkAndMarkLatePaymentsByFrequency(contract);
        checkAndCancelContractForLatePayments(contract);

        InsuranceContract updated = contractRepository.findById(contractId).get();
        System.out.println("✅ Nouveau statut: " + updated.getStatus());
    }

    /**
     * Simulation de retards pour tests
     */
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


    // Dans InsuranceContractService.java - Ajoutez cette méthode

    /**
     * Vérifie les contrats qui devraient être marqués comme COMPLETED
     * S'exécute quotidiennement à 00:30 (après la vérification des retards)
     */
    @Scheduled(cron = "0 30 0 * * ?") // 00:30 chaque jour
    @Transactional
    public void checkCompletedContracts() {
        System.out.println("✅ Vérification des contrats à marquer COMPLETED - " + new Date());

        // Vérifier les contrats ACTIFS
        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);

        for (InsuranceContract contract : activeContracts) {
            checkAndMarkContractAsCompleted(contract);
        }

        // Vérifier aussi les contrats INACTIFS (au cas où)
        List<InsuranceContract> inactiveContracts = contractRepository.findByStatus(ContractStatus.INACTIVE);
        for (InsuranceContract contract : inactiveContracts) {
            checkAndMarkContractAsCompleted(contract);
        }
    }

    /**
     * Vérifie et marque un contrat comme COMPLETED si toutes les conditions sont remplies
     */
    private void checkAndMarkContractAsCompleted(InsuranceContract contract) {
        if (contract.getPayments() == null || contract.getPayments().isEmpty()) {
            return;
        }

        Date today = new Date();
        Date endDate = contract.getEndDate();

        // Condition 1: La date de fin est dépassée
        boolean isEndDatePassed = endDate != null && endDate.before(today);

        if (!isEndDatePassed) {
            return; // La date de fin n'est pas encore dépassée
        }

        // Condition 2: Vérifier que tous les paiements sont PAID
        boolean allPaymentsPaid = true;
        int totalPayments = 0;
        int paidPayments = 0;

        for (Payment payment : contract.getPayments()) {
            totalPayments++;
            if (payment.getStatus() == PaymentStatus.PAID) {
                paidPayments++;
            } else {
                allPaymentsPaid = false;
                System.out.println("⚠️ Contrat " + contract.getContractId() +
                        " - Paiement non payé: " + payment.getPaymentId() +
                        " (statut: " + payment.getStatus() + ")");
            }
        }

        // Condition 3: Vérifier que le montant total payé correspond à la prime
        boolean totalPaidMatches = Math.abs(contract.getTotalPaid() - contract.getPremium()) < 0.01;

        System.out.println("📊 Contrat " + contract.getContractId() +
                " - Paiements: " + paidPayments + "/" + totalPayments +
                " | Total payé: " + contract.getTotalPaid() +
                " | Prime: " + contract.getPremium());

        // Si toutes les conditions sont remplies, marquer comme COMPLETED
        if (allPaymentsPaid && totalPaidMatches) {
            contract.setStatus(ContractStatus.COMPLETED);
            contractRepository.save(contract);

            System.out.println("🎉 CONTRAT " + contract.getContractId() +
                    " MARQUÉ COMPLETED - Tous les paiements effectués");
        } else if (isEndDatePassed && !allPaymentsPaid) {
            // Optionnel: Si la date est dépassée mais pas tous les paiements
            System.out.println("⚠️ Contrat " + contract.getContractId() +
                    " - Date dépassée mais paiements incomplets (" +
                    paidPayments + "/" + totalPayments + ")");
        }
    }

}