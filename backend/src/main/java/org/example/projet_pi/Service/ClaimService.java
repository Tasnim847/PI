package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Dto.*;
import org.example.projet_pi.Mapper.AutoClaimMapper;
import org.example.projet_pi.Mapper.ClaimMapper;
import org.example.projet_pi.Mapper.HealthClaimMapper;
import org.example.projet_pi.Mapper.HomeClaimMapper;
import org.example.projet_pi.Repository.*;
import org.example.projet_pi.entity.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    private final ClientScoringService clientScoringService;
    private final AdvancedClaimScoringService advancedClaimScoringService;

    /*
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
*/



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

        // ===============================
        // 🔥 CREATION CLAIM
        // ===============================
        Claim claim = new Claim();
        claim.setClaimDate(new Date());
        claim.setClaimedAmount(claimDTO.getClaimedAmount());
        claim.setApprovedAmount(0.0);
        claim.setDescription(claimDTO.getDescription());
        claim.setContract(contract);
        claim.setClient(client);
        claim.setStatus(ClaimStatus.IN_REVIEW);

        // ===============================
        // 🔥 AJOUT DES DETAILS SELON TYPE PRODUIT
        // ===============================
        // Récupération de l'enum
        ProductType type = contract.getProduct().getProductType();

        switch (type) {

            case AUTO:
                if (claimDTO.getAutoDetails() == null) {
                    throw new RuntimeException("Auto details obligatoire");
                }

                AutoClaimDetails auto =
                        AutoClaimMapper.toEntity(claimDTO.getAutoDetails());

                auto.setClaim(claim);
                claim.setAutoDetails(auto);
                break;

            case HEALTH:
                if (claimDTO.getHealthDetails() == null) {
                    throw new RuntimeException("Health details obligatoire");
                }

                HealthClaimDetails health =
                        HealthClaimMapper.toEntity(claimDTO.getHealthDetails());

                health.setClaim(claim);
                claim.setHealthDetails(health);
                break;

            case HOME:
                if (claimDTO.getHomeDetails() == null) {
                    throw new RuntimeException("Home details obligatoire");
                }

                HomeClaimDetails home =
                        HomeClaimMapper.toEntity(claimDTO.getHomeDetails());

                home.setClaim(claim);
                claim.setHomeDetails(home);
                break;

            case LIFE:
            case OTHER:
                // formulaire simple pour ces types
                break;

            default:
                throw new RuntimeException("Type de produit inconnu");
        }

        // ===============================
        // 📄 DOCUMENTS
        // ===============================
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

        // ===============================
        // 💾 SAVE
        // ===============================
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

        // 🔥 UPDATE AUTO / HEALTH / HOME
        if (claimDTO.getAutoDetails() != null) {
            AutoClaimDetails auto =
                    AutoClaimMapper.toEntity(claimDTO.getAutoDetails());
            auto.setClaim(claim);
            claim.setAutoDetails(auto);
        }

        if (claimDTO.getHealthDetails() != null) {
            HealthClaimDetails health =
                    HealthClaimMapper.toEntity(claimDTO.getHealthDetails());
            health.setClaim(claim);
            claim.setHealthDetails(health);
        }

        if (claimDTO.getHomeDetails() != null) {
            HomeClaimDetails home =
                    HomeClaimMapper.toEntity(claimDTO.getHomeDetails());
            home.setClaim(claim);
            claim.setHomeDetails(home);
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
    // Dans ClaimService.java - MODIFIER createCompensationForApprovedClaim

    @Transactional
    protected Compensation createCompensationForApprovedClaim(Claim claim) {
        // Vérifier qu'il n'y a pas déjà une compensation
        if (claim.getCompensation() != null) {
            log.info("⚠️ Le claim {} a déjà une compensation", claim.getClaimId());
            return claim.getCompensation();
        }

        // 🔥 NOUVEAU: Calculer le scoring avancé AVANT de créer la compensation
        ClaimScoreDTO claimScore = advancedClaimScoringService.calculateAdvancedClaimScore(claim.getClaimId());

        log.info("📊 Scoring avancé pour claim {}: score={}, niveau={}",
                claim.getClaimId(), claimScore.getRiskScore(), claimScore.getRiskLevel());

        InsuranceContract contract = claim.getContract();
        if (contract == null) {
            throw new RuntimeException("Le claim n'est pas lié à un contrat !");
        }

        // CALCUL DES MONTANTS
        double claimedAmount = claim.getClaimedAmount();
        double approvedAmount = claim.getApprovedAmount() > 0 ? claim.getApprovedAmount() : claimedAmount;
        double franchise = contract.getDeductible();
        double coverageLimit = contract.getCoverageLimit();

        // Calcul du montant pris en charge par l'assurance
        double afterFranchise = Math.max(0, approvedAmount - franchise);
        double baseInsurancePayment = Math.min(afterFranchise, coverageLimit);

        // 🔥 NOUVEAU: Ajustement basé sur le scoring
        double insurancePayment = baseInsurancePayment;
        double clientOutOfPocket = approvedAmount - insurancePayment;

        if (claimScore.getRiskScore() < 40) {
            // Risque élevé - pénalité
            double penalty = baseInsurancePayment * 0.15;
            insurancePayment = Math.max(0, baseInsurancePayment - penalty);
            clientOutOfPocket = approvedAmount - insurancePayment;
            log.warn("⚠️ Pénalité de risque: {} DT appliquée (score: {})", penalty, claimScore.getRiskScore());
        } else if (claimScore.getRiskScore() > 80) {
            // Risque très faible - bonus
            double bonus = baseInsurancePayment * 0.05;
            insurancePayment = Math.min(coverageLimit, baseInsurancePayment + bonus);
            clientOutOfPocket = approvedAmount - insurancePayment;
            log.info("✅ Bonus de risque: {} DT appliqué (score: {})", bonus, claimScore.getRiskScore());
        }

        // Mettre à jour le montant approuvé si nécessaire
        if (claim.getApprovedAmount() == 0) {
            claim.setApprovedAmount(approvedAmount);
        }

        // Générer message avec scoring
        String message = generateCompensationMessageWithScoring(
                approvedAmount, franchise, coverageLimit, insurancePayment,
                clientOutOfPocket, claimScore
        );

        // Création de la compensation avec scoring
        Compensation compensation = new Compensation();
        compensation.setAmount(insurancePayment);
        compensation.setAdjustedAmount(insurancePayment);
        compensation.setPaymentDate(new Date());
        compensation.setClaim(claim);
        compensation.setClientOutOfPocket(clientOutOfPocket);
        compensation.setCoverageLimit(coverageLimit);
        compensation.setDeductible(franchise);
        compensation.setOriginalClaimedAmount(claimedAmount);
        compensation.setApprovedAmount(approvedAmount);
        compensation.setMessage(message);
        compensation.setStatus(CompensationStatus.CALCULATED);
        compensation.setCalculationDate(new Date());

        // 🔥 STOCKER LE SCORING
        compensation.setRiskScore(claimScore.getRiskScore());
        compensation.setRiskLevel(claimScore.getRiskLevel());
        compensation.setDecisionSuggestion(claimScore.getDecisionSuggestion().toString());
        compensation.setScoringDetails(String.format(
                "Score: %d/100 | Niveau: %s | Décision: %s\nRecommandation: %s\nFacteurs: %s",
                claimScore.getRiskScore(),
                claimScore.getRiskLevel(),
                claimScore.getDecisionSuggestion(),
                claimScore.getRecommendation(),
                claimScore.getDelayInfo() + ", " + claimScore.getDocumentTypeInfo() + ", " + claimScore.getFrequencyInfo()
        ));

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
        log.info("   - Plafond contrat: {} DT", coverageLimit);
        log.info("   - Score risque: {} DT", claimScore.getRiskScore());
        log.info("   - Montant assurance: {} DT", insurancePayment);
        log.info("   - Reste à charge: {} DT", clientOutOfPocket);

        return compensation;
    }

    private String generateCompensationMessageWithScoring(double approvedAmount, double deductible,
                                                          double coverageLimit, double insurancePayment,
                                                          double clientOutOfPocket, ClaimScoreDTO score) {
        StringBuilder message = new StringBuilder();

        message.append("📋 DÉTAILS DU REMBOURSEMENT AVEC SCORING AVANCÉ\n");
        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // Calcul standard
        if (approvedAmount > coverageLimit) {
            message.append(String.format(
                    "⚠️ ATTENTION: Votre réclamation (%.2f DT) dépasse le plafond du contrat (%.2f DT).\n",
                    approvedAmount, coverageLimit
            ));
        }

        message.append(String.format("💰 Montant approuvé: %.2f DT\n", approvedAmount));
        message.append(String.format("🔧 Franchise appliquée: %.2f DT\n", deductible));
        message.append(String.format("📊 Plafond du contrat: %.2f DT\n", coverageLimit));
        message.append(String.format("🏦 Montant pris en charge: %.2f DT\n", insurancePayment));
        message.append(String.format("💳 Reste à votre charge: %.2f DT\n\n", clientOutOfPocket));

        // Scoring
        message.append("🎯 SCORING AVANCÉ\n");
        message.append("━━━━━━━━━━━━━━━━━━━━\n");
        message.append(String.format("📈 Score de risque: %d/100\n", score.getRiskScore()));
        message.append(String.format("⚠️ Niveau de risque: %s %s\n", score.getColorCode(), score.getRiskLevel()));
        message.append(String.format("🤖 Décision suggérée: %s\n", score.getDecisionSuggestion()));
        message.append(String.format("💡 Recommandation: %s\n\n", score.getRecommendation()));

        message.append("📋 Facteurs analysés:\n");
        message.append(String.format("   • Délai: %s\n", score.getDelayInfo()));
        message.append(String.format("   • Documents: %s\n", score.getDocumentTypeInfo()));
        message.append(String.format("   • Fréquence: %s\n", score.getFrequencyInfo()));

        return message.toString();
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

    // ===============================
    // 🔍 FRAUD DETECTION
    // ===============================
    public boolean isFraudulent(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        boolean highAmount = claim.getClaimedAmount() > 10000;
        boolean tooManyClaims =
                claimRepository.findByClientId(claim.getClient().getId()).size() > 5;

        return highAmount || tooManyClaims;
    }

    // ===============================
    // 📊 STATISTICS
    // ===============================
    public Map<String, Object> getStats() {
        List<Claim> claims = claimRepository.findAll();

        long total = claims.size();
        long approved = claims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.APPROVED)
                .count();
        long rejected = claims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.REJECTED)
                .count();

        double totalAmount = claims.stream()
                .mapToDouble(Claim::getClaimedAmount)
                .sum();

        return Map.of(
                "totalClaims", total,
                "approvedClaims", approved,
                "rejectedClaims", rejected,
                "totalAmount", totalAmount
        );
    }

    // ===============================
    // 🔍 SEARCH
    // ===============================
    public List<ClaimDTO> search(String status, Double min, Double max) {
        return claimRepository.findAll()
                .stream()
                .filter(c -> status == null || c.getStatus().name().equals(status))
                .filter(c -> min == null || c.getClaimedAmount() >= min)
                .filter(c -> max == null || c.getClaimedAmount() <= max)
                .map(ClaimMapper::toDTO)
                .collect(Collectors.toList());
    }


    // ===============================
    // 💡 RECOMMENDATION
    // ===============================
    public String getRecommendation(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        if (claim.getClaimedAmount() > claim.getContract().getCoverageLimit()) {
            return "⚠️ Montant dépasse le plafond du contrat";
        }

        if (isFraudulent(claimId)) {
            return "⚠️ Claim suspect, vérification nécessaire";
        }

        return "✅ Claim normal";
    }

    // ===============================
    // 📈 PREDICTION
    // ===============================
    public Double predictClientCost(Long clientId) {
        List<Claim> claims = claimRepository.findByClientId(clientId);

        return claims.stream()
                .mapToDouble(Claim::getClaimedAmount)
                .average()
                .orElse(0);
    }

    @Transactional
    public ClaimDTO decideClaimAutomatically(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim non trouvé"));

        Client client = claim.getClient();

        // 1. Calcul du score avancé
        ClientScoreResult scoreResult = clientScoringService.calculateClientScore(client.getId());

        // 2. Définir un seuil pour approuver ou rejeter automatiquement
        double thresholdApprove = 70.0; // score >= 70 -> approuvé
        double thresholdReject = 40.0;  // score < 40 -> rejeté

        ClaimDTO claimDTO = ClaimMapper.toDTO(claim);

        if (scoreResult.getGlobalScore() >= thresholdApprove) {
            // Approuver automatiquement
            claimDTO.setStatus(ClaimStatus.APPROVED.name());
            claimDTO.setApprovedAmount(claim.getClaimedAmount());
            return this.updateClaim(claimDTO, client.getEmail());
        } else if (scoreResult.getGlobalScore() < thresholdReject) {
            // Rejeter automatiquement
            claimDTO.setStatus(ClaimStatus.REJECTED.name());
            claimDTO.setDescription(claim.getDescription() + " [REJETÉ AUTOMATIQUE]");
            return this.updateClaim(claimDTO, client.getEmail());
        }

        // Sinon, garder IN_REVIEW
        return claimDTO;
    }

    // Dans ClaimService, ajoutez cette méthode améliorée

    @Transactional
    public ClaimDTO decideClaimAutomaticallyWithAdvancedScoring(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim non trouvé"));

        Client client = claim.getClient();

        // 1. Calcul du score avancé du claim
        ClaimScoreDTO claimScore = advancedClaimScoringService.calculateAdvancedClaimScore(claimId);

        log.info("📊 Scoring avancé du claim {}: score={}, décision suggérée={}",
                claimId, claimScore.getRiskScore(), claimScore.getDecisionSuggestion());

        // 2. Décision basée sur le scoring
        ClaimDTO claimDTO = ClaimMapper.toDTO(claim);

        switch (claimScore.getDecisionSuggestion()) {
            case AUTO_APPROVE:
                log.info("✅ Auto-approbation du claim {} (score: {})", claimId, claimScore.getRiskScore());
                claimDTO.setStatus(ClaimStatus.APPROVED.name());
                claimDTO.setApprovedAmount(claim.getClaimedAmount());
                claimDTO.setMessage("✅ Claim approuvé automatiquement après scoring avancé");
                return this.updateClaim(claimDTO, client.getEmail());

            case AUTO_REJECT:
                log.info("❌ Auto-rejet du claim {} (score: {})", claimId, claimScore.getRiskScore());
                claimDTO.setStatus(ClaimStatus.REJECTED.name());
                claimDTO.setDescription(claim.getDescription() +
                        String.format("\n[REJETÉ AUTOMATIQUEMENT] Score de risque: %d/100 - %s",
                                claimScore.getRiskScore(), claimScore.getRecommendation()));
                return this.updateClaim(claimDTO, client.getEmail());

            case MANUAL_REVIEW:
            default:
                log.info("⚠️ Revue manuelle requise pour le claim {} (score: {})",
                        claimId, claimScore.getRiskScore());
                claimDTO.setStatus(ClaimStatus.IN_REVIEW.name());
                claimDTO.setMessage(String.format(
                        "⚠️ Claim nécessite une revue manuelle. Score de risque: %d/100\n\n%s",
                        claimScore.getRiskScore(), claimScore.getRecommendation()));
                return this.updateClaim(claimDTO, client.getEmail());
        }
    }
}