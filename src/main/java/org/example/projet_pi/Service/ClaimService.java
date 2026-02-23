package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.CompensationDTO;
import org.example.projet_pi.Dto.DocumentDTO;
import org.example.projet_pi.Repository.*;
import org.example.projet_pi.Dto.ClaimDTO;
import org.example.projet_pi.entity.*;
import org.example.projet_pi.Mapper.ClaimMapper;
import org.example.projet_pi.Mapper.CompensationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ClaimService implements IClaimService {

    private final ClaimRepository claimRepository;
    private final InsuranceContractRepository contractRepository;
    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final CompensationRepository compensationRepository;



    @Transactional
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

        // Validation métier : pas plus d’un claim actif
        boolean hasActiveClaim = claimRepository.findAll()
                .stream()
                .filter(c -> c.getContract() != null && c.getClient() != null)
                .anyMatch(c -> c.getContract().getContractId().equals(contract.getContractId())
                        && c.getClient().getId().equals(client.getId())
                        && (c.getStatus() == ClaimStatus.IN_REVIEW || c.getStatus() == ClaimStatus.APPROVED));

        if (hasActiveClaim) {
            throw new RuntimeException("Ce client a déjà un claim actif pour ce contrat !");
        }

        // Création du Claim
        Claim claim = new Claim();
        // 🔥 MODIFICATION: Toujours utiliser la date du jour
        claim.setClaimDate(new Date());  // Date du jour, peu importe ce qui est envoyé
        claim.setClaimedAmount(claimDTO.getClaimedAmount());
        claim.setApprovedAmount(0.0);
        claim.setDescription(claimDTO.getDescription());
        claim.setContract(contract);
        claim.setClient(client);
        claim.setStatus(ClaimStatus.IN_REVIEW);

        // Création des documents
        List<Document> documents = new ArrayList<>();
        if (claimDTO.getDocuments() != null && !claimDTO.getDocuments().isEmpty()) {
            for (DocumentDTO docDTO : claimDTO.getDocuments()) {
                Document doc = new Document();
                doc.setName(docDTO.getName());
                doc.setType(docDTO.getType());
                doc.setFilePath(docDTO.getFilePath());

                // Pour les documents, on garde la date fournie ou la date du jour
                if (docDTO.getUploadDate() != null) {
                    doc.setUploadDate(docDTO.getUploadDate());
                } else {
                    doc.setUploadDate(LocalDateTime.now());
                }

                doc.setClaim(claim);
                doc.setClient(client);
                doc.setStatus(DocumentStatus.UPLOADED);
                documents.add(doc);
            }
        }

        if (documents.isEmpty()) {
            throw new RuntimeException("Au moins un document doit être fourni pour la déclaration !");
        }

        claim.setDocuments(documents);
        Claim savedClaim = claimRepository.save(claim);

        return ClaimMapper.toDTO(savedClaim);
    }

    @Override
    @Transactional
    public ClaimDTO updateClaim(ClaimDTO claimDTO) {
        if (claimDTO.getClaimId() == null) {
            throw new IllegalArgumentException("claimId ne peut pas être null !");
        }

        Claim claim = claimRepository.findById(claimDTO.getClaimId())
                .orElseThrow(() -> new RuntimeException("Claim introuvable !"));

        ClaimStatus oldStatus = claim.getStatus();
        ClaimStatus newStatus = claimDTO.getStatus() != null ?
                ClaimStatus.valueOf(claimDTO.getStatus()) : oldStatus;

        // Mise à jour des champs
        if (claimDTO.getClaimDate() != null) {
            claim.setClaimDate(claimDTO.getClaimDate());
        }
        if (claimDTO.getClaimedAmount() > 0) {
            claim.setClaimedAmount(claimDTO.getClaimedAmount());
        }
        if (claimDTO.getApprovedAmount() >= 0) {
            claim.setApprovedAmount(claimDTO.getApprovedAmount());
        }
        if (claimDTO.getDescription() != null) {
            claim.setDescription(claimDTO.getDescription());
        }

        // Mise à jour du statut
        claim.setStatus(newStatus);

        // Gestion des documents
        if (claimDTO.getDocumentIds() != null && !claimDTO.getDocumentIds().isEmpty()) {
            List<Document> newDocuments = new ArrayList<>();
            for (Long docId : claimDTO.getDocumentIds()) {
                Document doc = documentRepository.findById(docId)
                        .orElseThrow(() -> new RuntimeException("Document introuvable avec l'id: " + docId));
                doc.setClaim(claim);
                newDocuments.add(doc);
            }
            claim.getDocuments().clear();
            claim.getDocuments().addAll(newDocuments);
        }

        // Validation : documents requis pour IN_REVIEW
        if (newStatus == ClaimStatus.IN_REVIEW &&
                (claim.getDocuments() == null || claim.getDocuments().isEmpty())) {
            throw new RuntimeException("Au moins un document doit être fourni pour un claim en révision !");
        }

        // Sauvegarder d'abord le claim
        Claim updatedClaim = claimRepository.save(claim);

        // CRÉATION AUTOMATIQUE DE LA COMPENSATION SI LE CLAIM PASSE À APPROVED
        if (oldStatus != ClaimStatus.APPROVED && newStatus == ClaimStatus.APPROVED) {
            createCompensationForApprovedClaim(updatedClaim);
        }

        return ClaimMapper.toDTO(updatedClaim);
    }

    /**
     * Crée automatiquement une compensation pour un claim approuvé
     */
    @Transactional
    protected void createCompensationForApprovedClaim(Claim claim) {
        // Vérifier qu'il n'y a pas déjà une compensation
        if (claim.getCompensation() != null) {
            System.out.println("⚠️ Le claim " + claim.getClaimId() + " a déjà une compensation");
            return;
        }

        InsuranceContract contract = claim.getContract();
        if (contract == null) {
            throw new RuntimeException("Le claim n'est pas lié à un contrat !");
        }

        // CALCUL DES MONTANTS
        double claimedAmount = claim.getClaimedAmount();
        double approvedAmount = claim.getApprovedAmount() > 0 ?
                claim.getApprovedAmount() : claimedAmount;

        double franchise = contract.getDeductible();

        // Calcul du montant payé par l'assurance
        double insurancePayment = Math.max(0, approvedAmount - franchise);

        // La compensation ne peut pas dépasser le plafond du contrat
        if (insurancePayment > contract.getCoverageLimit()) {
            insurancePayment = contract.getCoverageLimit();
        }

        // Mettre à jour le montant approuvé si nécessaire
        if (claim.getApprovedAmount() == 0) {
            claim.setApprovedAmount(approvedAmount);
        }

        // Création de la compensation
        Compensation compensation = new Compensation();
        compensation.setAmount(insurancePayment);
        compensation.setPaymentDate(new Date());
        compensation.setClaim(claim);

        // Sauvegarde
        compensation = compensationRepository.save(compensation);

        // Lier la compensation au claim
        claim.setCompensation(compensation);
        claimRepository.save(claim);

        // Affichage des détails
        System.out.println("🎉 Compensation créée automatiquement pour le claim " + claim.getClaimId());
        System.out.println("   - Montant réclamé: " + claimedAmount + " DT");
        System.out.println("   - Montant approuvé: " + approvedAmount + " DT");
        System.out.println("   - Franchise client: " + franchise + " DT");
        System.out.println("   - Montant assurance: " + insurancePayment + " DT");
    }

    /**
     * Méthode pour approuver un claim manuellement
     */
    @Transactional
    public ClaimDTO approveClaim(Long claimId, Double approvedAmount) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim non trouvé avec l'id: " + claimId));

        if (claim.getStatus() != ClaimStatus.IN_REVIEW) {
            throw new RuntimeException("Seuls les claims en révision (IN_REVIEW) peuvent être approuvés !");
        }

        // Mettre à jour le montant approuvé si fourni
        if (approvedAmount != null && approvedAmount > 0) {
            claim.setApprovedAmount(approvedAmount);
        }

        claim.setStatus(ClaimStatus.APPROVED);
        Claim updatedClaim = claimRepository.save(claim);

        // Créer automatiquement la compensation
        createCompensationForApprovedClaim(updatedClaim);

        return ClaimMapper.toDTO(updatedClaim);
    }

    /**
     * Méthode pour rejeter un claim
     */
    @Transactional
    public ClaimDTO rejectClaim(Long claimId, String reason) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim non trouvé avec l'id: " + claimId));

        claim.setStatus(ClaimStatus.REJECTED);
        claim.setDescription(claim.getDescription() + " [REJETÉ: " + reason + "]");

        Claim updatedClaim = claimRepository.save(claim);
        return ClaimMapper.toDTO(updatedClaim);
    }

    /**
     * Méthode utilitaire pour obtenir les détails de compensation
     */
    public CompensationDetails getCompensationDetails(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim non trouvé avec l'id: " + claimId));

        InsuranceContract contract = claim.getContract();
        if (contract == null) {
            throw new RuntimeException("Le claim n'est pas lié à un contrat !");
        }

        double approvedAmount = claim.getApprovedAmount() > 0 ?
                claim.getApprovedAmount() : claim.getClaimedAmount();
        double franchise = contract.getDeductible();
        double insurancePayment = Math.max(0, approvedAmount - franchise);

        return new CompensationDetails(
                claimId,
                claim.getClaimedAmount(),
                approvedAmount,
                franchise,
                insurancePayment,
                claim.getStatus()
        );
    }

    @Override
    public void deleteClaim(Long id) {
        if (!claimRepository.existsById(id)) {
            throw new RuntimeException("Claim non trouvé avec l'id: " + id);
        }
        claimRepository.deleteById(id);
    }

    @Override
    public ClaimDTO getClaimById(Long id) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim non trouvé avec l'id: " + id));
        return ClaimMapper.toDTO(claim);
    }

    @Override
    public List<ClaimDTO> getAllClaims() {
        return claimRepository.findAll()
                .stream()
                .map(ClaimMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ========== CLASSE INTERNE POUR LES DÉTAILS ==========
    public static class CompensationDetails {
        private final Long claimId;
        private final double claimedAmount;
        private final double approvedAmount;
        private final double franchise;
        private final double insurancePayment;
        private final ClaimStatus status;

        public CompensationDetails(Long claimId, double claimedAmount,
                                   double approvedAmount, double franchise,
                                   double insurancePayment, ClaimStatus status) {
            this.claimId = claimId;
            this.claimedAmount = claimedAmount;
            this.approvedAmount = approvedAmount;
            this.franchise = franchise;
            this.insurancePayment = insurancePayment;
            this.status = status;
        }

        // Getters
        public Long getClaimId() { return claimId; }
        public double getClaimedAmount() { return claimedAmount; }
        public double getApprovedAmount() { return approvedAmount; }
        public double getFranchise() { return franchise; }
        public double getInsurancePayment() { return insurancePayment; }
        public ClaimStatus getStatus() { return status; }

        @Override
        public String toString() {
            return String.format(
                    "Claim %d: %s\n" +
                            "   Montant réclamé: %.2f DT\n" +
                            "   Montant approuvé: %.2f DT\n" +
                            "   Franchise client: %.2f DT\n" +
                            "   Montant assurance: %.2f DT",
                    claimId, status, claimedAmount, approvedAmount, franchise, insurancePayment
            );
        }
    }
}