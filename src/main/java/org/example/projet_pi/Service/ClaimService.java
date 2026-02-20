package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Repository.*;
import org.example.projet_pi.Dto.ClaimDTO;
import org.example.projet_pi.entity.*;
import org.example.projet_pi.Mapper.ClaimMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ClaimService implements IClaimService {

    private ClaimRepository claimRepository;
    private InsuranceContractRepository contractRepository;
    private CompensationRepository compensationRepository;
    private RiskClaimRepository riskClaimRepository;
    private DocumentRepository documentRepository;

    @Override
    public ClaimDTO addClaim(ClaimDTO claimDTO) {

        // Vérifier que contractId n'est pas null
        if (claimDTO.getContractId() == null) {
            throw new IllegalArgumentException("Le contractId ne peut pas être null !");
        }

        // Récupérer le contrat
        InsuranceContract contract = contractRepository.findById(claimDTO.getContractId())
                .orElseThrow(() -> new RuntimeException(
                        "Le contrat avec l'id " + claimDTO.getContractId() + " n'existe pas !"));

        // Créer le Claim
        Claim claim = new Claim();
        claim.setClaimDate(claimDTO.getClaimDate());
        claim.setClaimedAmount(claimDTO.getClaimedAmount());
        claim.setApprovedAmount(claimDTO.getApprovedAmount());
        claim.setDescription(claimDTO.getDescription());

        // Conversion String -> Enum
        if (claimDTO.getStatus() != null) {
            claim.setStatus(ClaimStatus.valueOf(claimDTO.getStatus()));
        }

        claim.setContract(contract);

        // Ajouter les documents si présents
        List<Document> documents = new ArrayList<>();
        if (claimDTO.getDocumentIds() != null) {
            for (Long docId : claimDTO.getDocumentIds()) {
                Document doc = documentRepository.findById(docId)
                        .orElseThrow(() -> new RuntimeException(
                                "Document avec l'id " + docId + " introuvable !"));
                documents.add(doc);
            }
        }
        claim.setDocuments(documents);

        // Sauvegarder le claim
        Claim savedClaim = claimRepository.save(claim);

        // Retourner le DTO
        ClaimDTO result = new ClaimDTO();
        result.setClaimId(savedClaim.getClaimId());
        result.setClaimDate(savedClaim.getClaimDate());
        result.setClaimedAmount(savedClaim.getClaimedAmount());
        result.setApprovedAmount(savedClaim.getApprovedAmount());
        result.setDescription(savedClaim.getDescription());
        result.setStatus(savedClaim.getStatus().name());
        result.setContractId(savedClaim.getContract().getContractId());

        if (!documents.isEmpty()) {
            List<Long> docIds = new ArrayList<>();
            for (Document d : documents) docIds.add(d.getDocumentId());
            result.setDocumentIds(docIds);
        }

        return result;
    }

    @Override
    public ClaimDTO updateClaim(ClaimDTO claimDTO) {
        if (claimDTO.getClaimId() == null) {
            throw new IllegalArgumentException("claimId ne peut pas être null !");
        }

        Claim claim = claimRepository.findById(claimDTO.getClaimId())
                .orElseThrow(() -> new RuntimeException("Claim introuvable !"));

        if (claimDTO.getClaimDate() != null) claim.setClaimDate(claimDTO.getClaimDate());
        claim.setClaimedAmount(claimDTO.getClaimedAmount());
        claim.setApprovedAmount(claimDTO.getApprovedAmount());
        if (claimDTO.getDescription() != null) claim.setDescription(claimDTO.getDescription());

        if (claimDTO.getStatus() != null) {
            claim.setStatus(ClaimStatus.valueOf(claimDTO.getStatus()));
        }

        // Mettre à jour documents si fournis
        if (claimDTO.getDocumentIds() != null) {
            List<Document> documents = new ArrayList<>();
            for (Long docId : claimDTO.getDocumentIds()) {
                Document doc = documentRepository.findById(docId)
                        .orElseThrow(() -> new RuntimeException("Document introuvable !"));
                documents.add(doc);
            }
            claim.setDocuments(documents);
        }

        Claim updatedClaim = claimRepository.save(claim);

        // Retour DTO
        ClaimDTO result = new ClaimDTO();
        result.setClaimId(updatedClaim.getClaimId());
        result.setClaimDate(updatedClaim.getClaimDate());
        result.setClaimedAmount(updatedClaim.getClaimedAmount());
        result.setApprovedAmount(updatedClaim.getApprovedAmount());
        result.setDescription(updatedClaim.getDescription());
        result.setStatus(updatedClaim.getStatus().name());
        result.setContractId(updatedClaim.getContract().getContractId());

        if (updatedClaim.getDocuments() != null) {
            List<Long> docIds = new ArrayList<>();
            for (Document d : updatedClaim.getDocuments()) docIds.add(d.getDocumentId());
            result.setDocumentIds(docIds);
        }

        return result;
    }

    @Override
    public void deleteClaim(Long id) {
        claimRepository.deleteById(id);
    }

    @Override
    public ClaimDTO getClaimById(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim not found"));
        return ClaimMapper.toDTO(claim);
    }

    @Override
    public List<ClaimDTO> getAllClaims() {
        return claimRepository.findAll()
                .stream()
                .map(ClaimMapper::toDTO)
                .collect(Collectors.toList());
    }

    private RiskClaim calculateRisk(Claim claim) {
        RiskClaim riskClaim = new RiskClaim();
        riskClaim.setClaim(claim);

        double score = claim.getClaimedAmount() / 1000; // Exemple
        riskClaim.setRiskScore(score);

        if (score < 5) {
            riskClaim.setRiskLevel("LOW");
        } else if (score < 10) {
            riskClaim.setRiskLevel("MEDIUM");
        } else {
            riskClaim.setRiskLevel("HIGH");
        }

        riskClaim.setEvaluationNote("Automatique : basé sur le montant réclamé");

        return riskClaim;
    }
}