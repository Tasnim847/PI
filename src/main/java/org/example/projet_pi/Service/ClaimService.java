package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Dto.ClaimDTO;
import org.example.projet_pi.Dto.CompensationDetailsDTO;
import org.example.projet_pi.Dto.DocumentDTO;
import org.example.projet_pi.Mapper.ClaimMapper;
import org.example.projet_pi.Repository.*;
import org.example.projet_pi.entity.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class ClaimService implements IClaimService {

    private final ClaimRepository claimRepository;
    private final InsuranceContractRepository contractRepository;
    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final CompensationRepository compensationRepository;
    private final UserRepository userRepository;  // Ajout du repository User
    private final EmailService emailService;

    @Override
    @Transactional
    public ClaimDTO addClaim(ClaimDTO claimDTO, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!(user instanceof Client)) {
            throw new AccessDeniedException("Seuls les clients peuvent créer des claims");
        }

        Client client = (Client) user;

        if (claimDTO.getContractId() == null) {
            throw new IllegalArgumentException("Le contractId est obligatoire");
        }

        InsuranceContract contract = contractRepository.findById(claimDTO.getContractId())
                .orElseThrow(() -> new RuntimeException(
                        "Le contrat avec l'id " + claimDTO.getContractId() + " n'existe pas"));

        if (!contract.getClient().getId().equals(client.getId())) {
            throw new AccessDeniedException("Ce contrat ne vous appartient pas");
        }

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Vous ne pouvez créer un claim que sur un contrat actif");
        }

        if (claimDTO.getClaimedAmount() == null || claimDTO.getClaimedAmount() <= 0) {
            throw new IllegalArgumentException("Le montant réclamé doit être supérieur à 0");
        }

        boolean hasActiveClaim =
                claimRepository.existsByContract_ContractIdAndClient_IdAndStatusIn(
                        contract.getContractId(),
                        client.getId(),
                        List.of(ClaimStatus.IN_REVIEW, ClaimStatus.APPROVED)
                );

        if (hasActiveClaim) {
            throw new RuntimeException("Vous avez déjà un claim actif pour ce contrat");
        }

        if (claimDTO.getDocuments() == null || claimDTO.getDocuments().isEmpty()) {
            throw new RuntimeException("Au moins un document est obligatoire");
        }

        // ✅ Message informatif si dépassement
        boolean exceedsLimit = claimDTO.getClaimedAmount() > contract.getCoverageLimit();

        Claim claim = new Claim();
        claim.setClaimDate(new Date());
        claim.setClaimedAmount(claimDTO.getClaimedAmount());
        claim.setApprovedAmount(0.0);
        claim.setDescription(claimDTO.getDescription());
        claim.setContract(contract);
        claim.setClient(client);
        claim.setStatus(ClaimStatus.IN_REVIEW);

        List<Document> documents = new ArrayList<>();

        for (DocumentDTO docDTO : claimDTO.getDocuments()) {

            Document doc = new Document();
            doc.setName(docDTO.getName());
            doc.setType(docDTO.getType());
            doc.setFilePath(docDTO.getFilePath());
            doc.setUploadDate(
                    docDTO.getUploadDate() != null ?
                            docDTO.getUploadDate() :
                            LocalDateTime.now()
            );

            doc.setStatus(DocumentStatus.UPLOADED);
            doc.setClaim(claim);
            doc.setClient(client);

            documents.add(doc);
        }

        claim.setDocuments(documents);

        Claim savedClaim = claimRepository.save(claim);

        ClaimDTO response = ClaimMapper.toDTO(savedClaim);

        // 🔥 Ajouter message informatif
        if (exceedsLimit) {
            response.setMessage("Attention : le montant dépasse le plafond du contrat.");
        }

        return response;
    }

    @Override
    @Transactional
    public ClaimDTO updateClaim(ClaimDTO claimDTO, String userEmail) {
        if (claimDTO.getClaimId() == null) {
            throw new IllegalArgumentException("claimId ne peut pas être null !");
        }

        Claim claim = claimRepository.findById(claimDTO.getClaimId())
                .orElseThrow(() -> new RuntimeException("Claim introuvable !"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérification des droits
        if (user instanceof Client) {
            // Un client ne peut modifier que ses propres claims et seulement s'ils sont IN_REVIEW
            if (!claim.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous ne pouvez modifier que vos propres claims");
            }
            if (claim.getStatus() != ClaimStatus.IN_REVIEW) {
                throw new RuntimeException("Vous ne pouvez modifier qu'un claim en cours de révision");
            }
        }

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

    @Override
    @Transactional
    public void deleteClaim(Long id, String userEmail) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim non trouvé avec l'id: " + id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérification des droits
        if (user instanceof Client) {
            if (!claim.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres claims");
            }
            if (claim.getStatus() != ClaimStatus.IN_REVIEW) {
                throw new RuntimeException("Vous ne pouvez supprimer qu'un claim en cours de révision");
            }
        }

        claimRepository.deleteById(id);
    }

    @Override
    public ClaimDTO getClaimById(Long id, String userEmail) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim non trouvé avec l'id: " + id));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérification des droits
        if (user instanceof Client) {
            if (!claim.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous ne pouvez consulter que vos propres claims");
            }
        } else if (user instanceof AgentAssurance) {
            AgentAssurance agent = (AgentAssurance) user;
            if (!claim.getClient().getAgentAssurance().getId().equals(agent.getId())) {
                throw new AccessDeniedException("Ce claim n'appartient pas à un de vos clients");
            }
        }
        // Admin peut tout voir

        return ClaimMapper.toDTO(claim);
    }

    @Override
    public List<ClaimDTO> getAllClaims(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user instanceof Client) {
            // Client: ses propres claims
            return claimRepository.findByClientId(user.getId())
                    .stream()
                    .map(ClaimMapper::toDTO)
                    .collect(Collectors.toList());
        } else if (user instanceof AgentAssurance) {
            // Agent: claims de ses clients
            AgentAssurance agent = (AgentAssurance) user;
            return claimRepository.findByContract_AgentAssuranceId(agent.getId())
                    .stream()
                    .map(ClaimMapper::toDTO)
                    .collect(Collectors.toList());
        }
        // Admin: tous les claims
        return claimRepository.findAll()
                .stream()
                .map(ClaimMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClaimDTO approveClaim(Long claimId, Double approvedAmount, String userEmail) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim non trouvé avec l'id: " + claimId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier que c'est un agent d'assurance
        if (!(user instanceof AgentAssurance)) {
            throw new AccessDeniedException("Seuls les agents d'assurance peuvent approuver des claims");
        }

        AgentAssurance agent = (AgentAssurance) user;

        // Vérifier que le claim appartient à un client de cet agent
        if (!claim.getClient().getAgentAssurance().getId().equals(agent.getId())) {
            throw new AccessDeniedException("Ce claim n'appartient pas à un de vos clients");
        }

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
        Compensation compensation = createCompensationForApprovedClaim(updatedClaim);

        // 📧 Envoyer un email au client
        try {
            emailService.sendClaimApprovedEmail(claim.getClient(), updatedClaim, compensation);
            log.info("✅ Email d'acceptation envoyé à {} pour le claim {}",
                    claim.getClient().getEmail(), claimId);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email d'acceptation: {}", e.getMessage());
        }

        return ClaimMapper.toDTO(updatedClaim);
    }

    @Override
    @Transactional
    public ClaimDTO rejectClaim(Long claimId, String reason, String userEmail) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim non trouvé avec l'id: " + claimId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier que c'est un agent d'assurance
        if (!(user instanceof AgentAssurance)) {
            throw new AccessDeniedException("Seuls les agents d'assurance peuvent rejeter des claims");
        }

        AgentAssurance agent = (AgentAssurance) user;

        // Vérifier que le claim appartient à un client de cet agent
        if (!claim.getClient().getAgentAssurance().getId().equals(agent.getId())) {
            throw new AccessDeniedException("Ce claim n'appartient pas à un de vos clients");
        }

        claim.setStatus(ClaimStatus.REJECTED);
        claim.setDescription(claim.getDescription() + " [REJETÉ: " + reason + "]");

        Claim updatedClaim = claimRepository.save(claim);

        // 📧 Envoyer un email au client
        try {
            emailService.sendClaimRejectedEmail(claim.getClient(), updatedClaim, reason);
            log.info("✅ Email de rejet envoyé à {} pour le claim {} (raison: {})",
                    claim.getClient().getEmail(), claimId, reason);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de rejet: {}", e.getMessage());
        }

        return ClaimMapper.toDTO(updatedClaim);
    }



    /**
     * Crée automatiquement une compensation pour un claim approuvé
     */
    /**
     * Crée automatiquement une compensation pour un claim approuvé
     */
    @Transactional
    protected Compensation createCompensationForApprovedClaim(Claim claim) {
        // Vérifier qu'il n'y a pas déjà une compensation
        if (claim.getCompensation() != null) {
            System.out.println("⚠️ Le claim " + claim.getClaimId() + " a déjà une compensation");
            return claim.getCompensation();
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
        claim.setStatus(ClaimStatus.COMPENSATED);
        claimRepository.save(claim);

        // Affichage des détails
        log.info("🎉 Compensation créée automatiquement pour le claim {}", claim.getClaimId());
        log.info("   - Montant réclamé: {} DT", claimedAmount);
        log.info("   - Montant approuvé: {} DT", approvedAmount);
        log.info("   - Franchise client: {} DT", franchise);
        log.info("   - Montant assurance: {} DT", insurancePayment);

        return compensation;
    }

    @Override
    public CompensationDetailsDTO getCompensationDetails(Long claimId) {
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

        return new CompensationDetailsDTO(
                claimId,
                claim.getClaimedAmount(),
                approvedAmount,
                franchise,
                insurancePayment,
                claim.getStatus()
        );
    }
}