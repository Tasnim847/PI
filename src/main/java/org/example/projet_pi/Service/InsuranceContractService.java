package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.InsuranceContractDTO;
import org.example.projet_pi.Mapper.InsuranceContractMapper;
import org.example.projet_pi.Repository.AgentAssuranceRepository;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Repository.InsuranceProductRepository;
import org.example.projet_pi.entity.AgentAssurance;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.InsuranceContract;
import org.example.projet_pi.entity.InsuranceProduct;
import org.springframework.stereotype.Service;

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
        InsuranceContract contract = InsuranceContractMapper.toEntity(dto);

        // Récupérer les références depuis la base
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));
        AgentAssurance agent = agentRepository.findById(dto.getAgentAssuranceId())
                .orElseThrow(() -> new RuntimeException("Agent not found"));
        InsuranceProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        contract.setClient(client);
        contract.setAgentAssurance(agent);
        contract.setProduct(product);

        contract = contractRepository.save(contract);

        return InsuranceContractMapper.toDTO(contract);
    }

    @Override
    public InsuranceContractDTO updateContract(InsuranceContractDTO dto) {
        InsuranceContract contract = contractRepository.findById(dto.getContractId())
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Mettre à jour les champs simples
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setPremium(dto.getPremium());
        contract.setDeductible(dto.getDeductible());
        contract.setCoverageLimit(dto.getCoverageLimit());

        if (dto.getStatus() != null)
            contract.setStatus(Enum.valueOf(org.example.projet_pi.entity.ContractStatus.class, dto.getStatus()));

        // Mettre à jour les références
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