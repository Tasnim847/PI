package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.DocumentDTO;
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
    private DocumentRepository documentRepository;
    private ClientRepository clientRepository;

    @Override
    public ClaimDTO addClaim(ClaimDTO claimDTO) {
        // Vérification des IDs essentiels
        if (claimDTO.getContractId() == null) {
            throw new IllegalArgumentException("Le contractId ne peut pas être null !");
        }
        if (claimDTO.getClientId() == null) {
            throw new IllegalArgumentException("Le clientId ne peut pas être null !");
        }

        // Récupérer le contrat et le client
        InsuranceContract contract = contractRepository.findById(claimDTO.getContractId())
                .orElseThrow(() -> new RuntimeException(
                        "Le contrat avec l'id " + claimDTO.getContractId() + " n'existe pas !"));

        Client client = clientRepository.findById(claimDTO.getClientId())
                .orElseThrow(() -> new RuntimeException(
                        "Le client avec l'id " + claimDTO.getClientId() + " n'existe pas !"));

        // Validation métier : montant ne doit pas dépasser le plafond
        if (claimDTO.getClaimedAmount() > contract.getCoverageLimit()) {
            throw new RuntimeException("Le montant réclamé dépasse le plafond du contrat !");
        }

        // Validation métier : pas plus d’un claim actif pour ce client et ce contrat
        boolean hasActiveClaim = claimRepository.findAll()
                .stream()
                .filter(c -> c.getContract() != null && c.getClient() != null) // Protection null
                .anyMatch(c -> c.getContract().getContractId().equals(contract.getContractId())
                        && c.getClient().getId().equals(client.getId())
                        && (c.getStatus() == ClaimStatus.DECLARED || c.getStatus() == ClaimStatus.IN_REVIEW));
        if (hasActiveClaim) {
            throw new RuntimeException("Ce client a déjà un claim actif pour ce contrat !");
        }

        // Création du Claim
        Claim claim = new Claim();
        claim.setClaimDate(claimDTO.getClaimDate());
        claim.setClaimedAmount(claimDTO.getClaimedAmount());
        claim.setApprovedAmount(claimDTO.getApprovedAmount());
        claim.setDescription(claimDTO.getDescription());
        claim.setContract(contract);
        claim.setClient(client);
        claim.setStatus(claimDTO.getStatus() != null ? ClaimStatus.valueOf(claimDTO.getStatus()) : ClaimStatus.DECLARED);

        // Création des documents depuis DocumentDTO
        List<Document> documents = new ArrayList<>();
        if (claimDTO.getDocuments() != null) {
            for (DocumentDTO docDTO : claimDTO.getDocuments()) {
                Document doc = new Document();
                doc.setName(docDTO.getName());
                doc.setType(docDTO.getType());
                doc.setFilePath(docDTO.getFilePath());
                doc.setUploadDate(docDTO.getUploadDate());
                doc.setClaim(claim);   // liaison au claim
                doc.setClient(client); // liaison au client
                documents.add(doc);
            }
        }

        // Validation métier : documents obligatoires si IN_REVIEW
        if (claim.getStatus() == ClaimStatus.IN_REVIEW && documents.isEmpty()) {
            throw new RuntimeException("Tous les documents obligatoires doivent être fournis pour IN_REVIEW !");
        }

        claim.setDocuments(documents);


        // Sauvegarder le claim (cascade sauvegardera aussi les documents et RiskClaim)
        Claim savedClaim = claimRepository.save(claim);

        return ClaimMapper.toDTO(savedClaim);
    }

    @Override
    public ClaimDTO updateClaim(ClaimDTO claimDTO) {
        if (claimDTO.getClaimId() == null) throw new IllegalArgumentException("claimId ne peut pas être null !");

        Claim claim = claimRepository.findById(claimDTO.getClaimId())
                .orElseThrow(() -> new RuntimeException("Claim introuvable !"));

        // Champs simples
        if (claimDTO.getClaimDate() != null) claim.setClaimDate(claimDTO.getClaimDate());
        claim.setClaimedAmount(claimDTO.getClaimedAmount());
        claim.setApprovedAmount(claimDTO.getApprovedAmount());
        if (claimDTO.getDescription() != null) claim.setDescription(claimDTO.getDescription());

        // Statut
        if (claimDTO.getStatus() != null) claim.setStatus(ClaimStatus.valueOf(claimDTO.getStatus()));

        // Documents (ajout / suppression automatique grâce à orphanRemoval)
        if (claimDTO.getDocumentIds() != null) {
            List<Document> newDocuments = new ArrayList<>();
            for (Long docId : claimDTO.getDocumentIds()) {
                Document doc = documentRepository.findById(docId)
                        .orElseThrow(() -> new RuntimeException("Document introuvable !"));
                doc.setClaim(claim);
                newDocuments.add(doc);
            }
            claim.getDocuments().clear();
            claim.getDocuments().addAll(newDocuments);
        }

        if (claim.getStatus() == ClaimStatus.IN_REVIEW && (claim.getDocuments() == null || claim.getDocuments().isEmpty())) {
            throw new RuntimeException("Tous les documents obligatoires doivent être fournis pour IN_REVIEW !");
        }

        Claim updatedClaim = claimRepository.save(claim);


        claimRepository.save(updatedClaim);

        return ClaimMapper.toDTO(updatedClaim);
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
}