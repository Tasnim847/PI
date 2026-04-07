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
        dto.setTotalPaid(contract.getTotalPaid());
        dto.setRemainingAmount(contract.getRemainingAmount());
        dto.setStatus(contract.getStatus() != null ? contract.getStatus().name() : null);
        dto.setPaymentFrequency(contract.getPaymentFrequency() != null ? contract.getPaymentFrequency().name() : null);

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

    // DTO -> Entity
    public static InsuranceContract toEntity(InsuranceContractDTO dto) {
        if (dto == null) return null;

        InsuranceContract contract = new InsuranceContract();
        contract.setContractId(dto.getContractId());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setPremium(dto.getPremium());
        contract.setDeductible(dto.getDeductible());
        contract.setCoverageLimit(dto.getCoverageLimit());

        // ✅ PROTECTION CONTRE NULL
        contract.setTotalPaid(
                dto.getTotalPaid() != null ? dto.getTotalPaid() : 0.0
        );

        contract.setRemainingAmount(
                dto.getRemainingAmount() != null
                        ? dto.getRemainingAmount()
                        : dto.getPremium()
        );

        if (dto.getStatus() != null) {
            contract.setStatus(
                    org.example.projet_pi.entity.ContractStatus.valueOf(dto.getStatus())
            );
        }

        if (dto.getPaymentFrequency() != null) {
            contract.setPaymentFrequency(
                    org.example.projet_pi.entity.PaymentFrequency.valueOf(dto.getPaymentFrequency())
            );
        }

        return contract;
    }
}