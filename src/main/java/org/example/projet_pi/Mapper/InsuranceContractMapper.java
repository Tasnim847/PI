package org.example.projet_pi.Mapper;

import org.example.projet_pi.Dto.InsuranceContractDTO;
import org.example.projet_pi.entity.InsuranceContract;

import java.util.stream.Collectors;

public class InsuranceContractMapper {

    // Entity -> DTO
    public static InsuranceContractDTO toDTO(InsuranceContract contract) {
        if (contract == null) return null;

        InsuranceContractDTO dto = new InsuranceContractDTO();
        dto.setContractId(contract.getContractId());
        dto.setStartDate(contract.getStartDate());
        dto.setEndDate(contract.getEndDate());
        dto.setPremium(contract.getPremium());
        dto.setDeductible(contract.getDeductible());
        dto.setCoverageLimit(contract.getCoverageLimit());
        dto.setStatus(contract.getStatus().name());

        if (contract.getClient() != null) dto.setClientId(contract.getClient().getId());
        if (contract.getProduct() != null) dto.setProductId(contract.getProduct().getProductId());
        if (contract.getAgentAssurance() != null) dto.setAgentAssuranceId(contract.getAgentAssurance().getId());

        if (contract.getClaims() != null) {
            dto.setClaimIds(contract.getClaims().stream()
                    .map(c -> c.getClaimId())
                    .collect(Collectors.toList()));
        }

        if (contract.getPayments() != null) {
            dto.setPaymentIds(contract.getPayments().stream()
                    .map(p -> p.getPaymentId())
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    // DTO -> Entity (attention aux relations à gérer depuis le Service)
    public static InsuranceContract toEntity(InsuranceContractDTO dto) {
        if (dto == null) return null;

        InsuranceContract contract = new InsuranceContract();
        contract.setContractId(dto.getContractId());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setPremium(dto.getPremium());
        contract.setDeductible(dto.getDeductible());
        contract.setCoverageLimit(dto.getCoverageLimit());

        // status à gérer
        if (dto.getStatus() != null) {
            contract.setStatus(Enum.valueOf(org.example.projet_pi.entity.ContractStatus.class, dto.getStatus()));
        }

        return contract;
    }
}