package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.RiskClaimDTO;
import org.example.projet_pi.Mapper.RiskClaimMapper;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Repository.RiskClaimRepository;
import org.example.projet_pi.entity.InsuranceContract;
import org.example.projet_pi.entity.RiskClaim;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class RiskClaimService implements IRiskClaimService {

    private RiskClaimRepository riskClaimRepository;
    private InsuranceContractRepository contractRepository;

    @Override
    public RiskClaimDTO addRiskClaim(RiskClaimDTO dto) {

        RiskClaim riskClaim = RiskClaimMapper.toEntity(dto);

        if (dto.getContractId() != null) {
            InsuranceContract contract = contractRepository.findById(dto.getContractId())
                    .orElseThrow(() -> new RuntimeException("Contract not found"));
            riskClaim.setContract(contract);
        }

        riskClaim = riskClaimRepository.save(riskClaim);

        return RiskClaimMapper.toDTO(riskClaim);
    }

    @Override
    public RiskClaimDTO updateRiskClaim(RiskClaimDTO dto) {

        RiskClaim riskClaim = riskClaimRepository.findById(dto.getRiskId())
                .orElseThrow(() -> new RuntimeException("Risk not found"));

        riskClaim.setRiskScore(dto.getRiskScore());
        riskClaim.setRiskLevel(dto.getRiskLevel());
        riskClaim.setEvaluationNote(dto.getEvaluationNote());

        riskClaim = riskClaimRepository.save(riskClaim);

        return RiskClaimMapper.toDTO(riskClaim);
    }

    @Override
    public void deleteRiskClaim(Long id) {
        riskClaimRepository.deleteById(id);
    }

    @Override
    public RiskClaimDTO getRiskClaimById(Long id) {
        RiskClaim riskClaim = riskClaimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Risk not found"));
        return RiskClaimMapper.toDTO(riskClaim);
    }

    @Override
    public List<RiskClaimDTO> getAllRiskClaims() {
        return riskClaimRepository.findAll()
                .stream()
                .map(RiskClaimMapper::toDTO)
                .toList();
    }
}