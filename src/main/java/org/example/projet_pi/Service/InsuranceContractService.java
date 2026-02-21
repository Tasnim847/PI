package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.InsuranceContractDTO;
import org.example.projet_pi.Mapper.InsuranceContractMapper;
import org.example.projet_pi.Repository.AgentAssuranceRepository;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Repository.InsuranceProductRepository;
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

    @Override
    public InsuranceContractDTO addContract(InsuranceContractDTO dto) {
        // Conversion DTO -> Entity
        InsuranceContract contract = InsuranceContractMapper.toEntity(dto);

        // 🔹 Vérification logique métier de base
        if (contract.getStartDate() == null || contract.getEndDate() == null) {
            throw new RuntimeException("Les dates de début et fin doivent être fournies !");
        }
        if (contract.getEndDate().before(contract.getStartDate())) {
            throw new RuntimeException("La date de fin doit être après la date de début !");
        }
        if (contract.getPremium() <= 0) {
            throw new RuntimeException("La prime doit être positive !");
        }
        if (dto.getPaymentFrequency() == null) {
            throw new RuntimeException("La fréquence de paiement doit être définie !");
        }

        // 🔹 Récupérer les références depuis la base
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));
        AgentAssurance agent = agentRepository.findById(dto.getAgentAssuranceId())
                .orElseThrow(() -> new RuntimeException("Agent not found"));
        InsuranceProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        contract.setClient(client);
        contract.setAgentAssurance(agent);
        contract.setProduct(product);

        // 🔹 Définir la fréquence de paiement
        contract.setPaymentFrequency(
                Enum.valueOf(org.example.projet_pi.entity.PaymentFrequency.class, dto.getPaymentFrequency())
        );

        // 🔹 Sauvegarder le contrat
        contract = contractRepository.save(contract);

        // 🔹 Générer les paiements planifiés selon la fréquence choisie
        generateScheduledPayments(contract);

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
}