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
    private final RiskClaimRepository riskClaimRepository;

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

        // 🔥 CALCUL DU RISQUE AVANT SAUVEGARDE
        RiskClaim riskClaim = calculateRisk(contract);

        // Lier bidirectionnellement
        riskClaim.setContract(contract);
        contract.setRiskClaim(riskClaim);

        // 🔥 Si HIGH ➜ Contrat annulé automatiquement
        if ("HIGH".equals(riskClaim.getRiskLevel())) {
            contract.setStatus(ContractStatus.CANCELLED);
        } else {
            contract.setStatus(ContractStatus.ACTIVE);
        }

        // ✅ UNE SEULE SAUVEGARDE (cascade enregistre RiskClaim)
        contract = contractRepository.save(contract);

        return InsuranceContractMapper.toDTO(contract);
    }
    /**
     * Génère automatiquement les paiements planifiés selon la fréquence de paiement du contrat
     */
    private void generateScheduledPayments(InsuranceContract contract) {
        if (contract.getPaymentFrequency() == null) return;

        List<Payment> payments = new ArrayList<>();
        Date start = contract.getStartDate();
        Date end = contract.getEndDate();
        double installment = contract.calculateInstallmentAmount();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);

        while (calendar.getTime().before(end) || calendar.getTime().equals(end)) {
            Payment payment = new Payment();
            payment.setContract(contract);
            payment.setAmount(installment);
            payment.setPaymentDate(calendar.getTime());
            payment.setStatus(org.example.projet_pi.entity.PaymentStatus.PENDING);
            payment.setPaymentMethod("DEFAULT"); // ou à renseigner selon choix du client
            payments.add(payment);

            // Avancer la date selon la fréquence
            switch (contract.getPaymentFrequency()) {
                case MONTHLY -> calendar.add(Calendar.MONTH, 1);
                case SEMI_ANNUAL -> calendar.add(Calendar.MONTH, 6);
                case ANNUAL -> calendar.add(Calendar.YEAR, 1);
            }
        }

        // Sauvegarder tous les paiements
        contract.getPayments().addAll(payments);
    }

    @Override
    public InsuranceContractDTO updateContract(InsuranceContractDTO dto) {
        InsuranceContract contract = contractRepository.findById(dto.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // 🔹 Mettre à jour les champs simples
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setPremium(dto.getPremium());
        contract.setDeductible(dto.getDeductible());
        contract.setCoverageLimit(dto.getCoverageLimit());

        if (dto.getStatus() != null) {
            contract.setStatus(Enum.valueOf(ContractStatus.class, dto.getStatus()));
        }

        // 🔹 Mettre à jour la fréquence de paiement
        if (dto.getPaymentFrequency() != null) {
            PaymentFrequency newFrequency = Enum.valueOf(PaymentFrequency.class, dto.getPaymentFrequency());
            if (contract.getPaymentFrequency() != newFrequency) {
                contract.setPaymentFrequency(newFrequency);

                // ⚡ Re-générer les paiements planifiés si la fréquence a changé
                regenerateScheduledPayments(contract);
            }
        }

        // 🔹 Mettre à jour les références
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

        contract = contractRepository.save(contract);

        return InsuranceContractMapper.toDTO(contract);
    }

    /**
     * Régénère les paiements planifiés selon la nouvelle fréquence
     * tout en conservant les paiements déjà effectués.
     */
    private void regenerateScheduledPayments(InsuranceContract contract) {
        List<Payment> existingPayments = contract.getPayments();
        // On peut filtrer uniquement les paiements PENDING
        existingPayments.removeIf(p -> p.getStatus() == PaymentStatus.PENDING);

        // Générer les nouveaux paiements
        generateScheduledPayments(contract);
    }

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
        return contractRepository.findAll().stream()
                .map(InsuranceContractMapper::toDTO)
                .collect(Collectors.toList());
    }

    private RiskClaim calculateRisk(InsuranceContract contract) {

        RiskClaim riskClaim = new RiskClaim();
        riskClaim.setContract(contract);

        double score = 0;

        // 🔹 Logique métier intelligente

        // Prime élevée = risque élevé
        if (contract.getPremium() > 10000) {
            score += 40;
        } else if (contract.getPremium() > 5000) {
            score += 25;
        } else {
            score += 10;
        }

        // Franchise faible = risque plus élevé
        if (contract.getDeductible() < 200) {
            score += 30;
        } else {
            score += 10;
        }

        // Plafond élevé = plus risqué
        if (contract.getCoverageLimit() > 50000) {
            score += 30;
        } else {
            score += 10;
        }

        riskClaim.setRiskScore(score);

        // Déterminer le niveau
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