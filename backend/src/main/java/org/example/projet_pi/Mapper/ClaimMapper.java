package org.example.projet_pi.Mapper;

import org.example.projet_pi.Dto.ClaimDTO;
import org.example.projet_pi.Dto.ClientDTO;
import org.example.projet_pi.Dto.DocumentDTO;
import org.example.projet_pi.entity.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClaimMapper {

    // =========================
    // Entity -> DTO
    // =========================
    public static ClaimDTO toDTO(Claim claim) {
        if (claim == null) return null;

        ClaimDTO dto = new ClaimDTO();
        dto.setClaimId(claim.getClaimId());
        dto.setClaimDate(claim.getClaimDate());
        dto.setClaimedAmount(claim.getClaimedAmount());
        dto.setApprovedAmount(claim.getApprovedAmount());
        dto.setDescription(claim.getDescription());
        dto.setStatus(claim.getStatus() != null ? claim.getStatus().name() : null);
        dto.setMessage(claim.getMessage());

        // ✅ CORRECTION: Set the client object instead of clientId
        if (claim.getClient() != null) {
            ClientDTO clientDTO = new ClientDTO();
            clientDTO.setId(claim.getClient().getId());
            clientDTO.setFirstName(claim.getClient().getFirstName());
            clientDTO.setLastName(claim.getClient().getLastName());
            clientDTO.setEmail(claim.getClient().getEmail());
            clientDTO.setTelephone(claim.getClient().getTelephone());
            dto.setClient(clientDTO);
        }

        if (claim.getContract() != null) {
            dto.setContractId(claim.getContract().getContractId());
        }

        if (claim.getCompensation() != null) {
            dto.setCompensationId(claim.getCompensation().getCompensationId());
        }

        // Documents
        if (claim.getDocuments() != null) {
            List<DocumentDTO> documentDTOs = new ArrayList<>();
            for (Document doc : claim.getDocuments()) {
                DocumentDTO docDTO = new DocumentDTO();
                docDTO.setDocumentId(doc.getDocumentId());
                docDTO.setName(doc.getName());
                docDTO.setType(doc.getType());
                docDTO.setFilePath(doc.getFilePath());
                docDTO.setUploadDate(doc.getUploadDate());
                docDTO.setStatus(doc.getStatus() != null ? doc.getStatus().name() : null);
                documentDTOs.add(docDTO);
            }
            dto.setDocuments(documentDTOs);
            dto.setDocumentIds(claim.getDocuments()
                    .stream()
                    .map(Document::getDocumentId)
                    .collect(Collectors.toList()));
        }

        // Détails selon type produit
        if (claim.getAutoDetails() != null) {
            dto.setAutoDetails(AutoClaimMapper.toDTO(claim.getAutoDetails()));
        }
        if (claim.getHealthDetails() != null) {
            dto.setHealthDetails(HealthClaimMapper.toDTO(claim.getHealthDetails()));
        }
        if (claim.getHomeDetails() != null) {
            dto.setHomeDetails(HomeClaimMapper.toDTO(claim.getHomeDetails()));
        }

        return dto;
    }

    // =========================
    // DTO -> Entity
    // =========================
    public static Claim toEntity(ClaimDTO dto,
                                 InsuranceContract contract,
                                 Client client,
                                 List<Document> documents) {
        if (dto == null) return null;

        Claim claim = new Claim();
        claim.setClaimId(dto.getClaimId());
        claim.setClaimDate(dto.getClaimDate());
        claim.setClaimedAmount(dto.getClaimedAmount());
        claim.setApprovedAmount(dto.getApprovedAmount());
        claim.setDescription(dto.getDescription());
        claim.setContract(contract);

        // Use the client from parameter (passed from service)
        claim.setClient(client);
        claim.setDocuments(documents);

        if (dto.getStatus() != null) {
            claim.setStatus(ClaimStatus.valueOf(dto.getStatus()));
        }

        // Détails selon type
        if (dto.getAutoDetails() != null) {
            AutoClaimDetails auto = AutoClaimMapper.toEntity(dto.getAutoDetails());
            auto.setClaim(claim);
            claim.setAutoDetails(auto);
        }
        if (dto.getHealthDetails() != null) {
            HealthClaimDetails health = HealthClaimMapper.toEntity(dto.getHealthDetails());
            health.setClaim(claim);
            claim.setHealthDetails(health);
        }
        if (dto.getHomeDetails() != null) {
            HomeClaimDetails home = HomeClaimMapper.toEntity(dto.getHomeDetails());
            home.setClaim(claim);
            claim.setHomeDetails(home);
        }

        return claim;
    }
}