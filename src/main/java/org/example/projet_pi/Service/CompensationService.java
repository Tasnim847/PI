package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Repository.CompensationRepository;
import org.example.projet_pi.Repository.ClaimRepository;
import org.example.projet_pi.Dto.CompensationDTO;
import org.example.projet_pi.entity.ClaimStatus;
import org.example.projet_pi.entity.Compensation;
import org.example.projet_pi.entity.Claim;
import org.example.projet_pi.Mapper.CompensationMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CompensationService implements ICompensationService {

    private final CompensationRepository compensationRepository;
    private final ClaimRepository claimRepository;

    @Override
    public CompensationDTO addCompensation(CompensationDTO dto) {

        if (dto.getClaimId() == null) {
            throw new IllegalArgumentException("claimId ne peut pas être null !");
        }

        Claim claim = claimRepository.findById(dto.getClaimId())
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        // ✅ RÈGLE MÉTIER
        if (claim.getStatus() != ClaimStatus.APPROVED) {
            throw new RuntimeException("La compensation ne peut être ajoutée que si le claim est APPROVED !");
        }

        // Vérifier qu'il n'y a pas déjà une compensation
        if (claim.getCompensation() != null) {
            throw new RuntimeException("Ce claim possède déjà une compensation !");
        }

        Compensation compensation = CompensationMapper.toEntity(dto, claim);
        compensation = compensationRepository.save(compensation);

        // ✅ Mettre automatiquement le status du claim à COMPENSATED
        claim.setStatus(ClaimStatus.COMPENSATED);
        claim.setCompensation(compensation);
        claimRepository.save(claim);

        return CompensationMapper.toDTO(compensation);
    }

    @Override
    public CompensationDTO updateCompensation(CompensationDTO dto) {
        Compensation compensation = compensationRepository.findById(dto.getCompensationId())
                .orElseThrow(() -> new RuntimeException("Compensation not found"));

        compensation.setAmount(dto.getAmount());
        compensation.setPaymentDate(dto.getPaymentDate());

        // Optionnel : mettre à jour le claim si nécessaire
        if (dto.getClaimId() != null) {
            Claim claim = claimRepository.findById(dto.getClaimId())
                    .orElseThrow(() -> new RuntimeException("Claim not found"));
            compensation.setClaim(claim);
        }

        compensation = compensationRepository.save(compensation);

        return CompensationMapper.toDTO(compensation);
    }

    @Override
    public void deleteCompensation(Long id) {
        compensationRepository.deleteById(id);
    }

    @Override
    public CompensationDTO getCompensationById(Long id) {
        Compensation compensation = compensationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compensation not found"));
        return CompensationMapper.toDTO(compensation);
    }

    @Override
    public List<CompensationDTO> getAllCompensations() {
        return compensationRepository.findAll().stream()
                .map(CompensationMapper::toDTO)
                .collect(Collectors.toList());
    }
}