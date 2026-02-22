package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.InsuranceContractDTO;
import org.example.projet_pi.Mapper.InsuranceContractMapper;
import org.example.projet_pi.Repository.*;
import org.example.projet_pi.entity.*;
import org.springframework.stereotype.Service;

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

        if ("HIGH".equals(riskClaim.getRiskLevel())) {
            contract.setStatus(ContractStatus.CANCELLED);
        } else {
            contract.setStatus(ContractStatus.ACTIVE);
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
}