package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Dto.ClientScoreResult;
import org.example.projet_pi.Dto.InsuranceContractDTO;
import org.example.projet_pi.Mapper.InsuranceContractMapper;
import org.example.projet_pi.Repository.*;
import org.example.projet_pi.entity.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class InsuranceContractService implements IInsuranceContractService {

    private final InsuranceContractRepository contractRepository;
    private final ClientRepository clientRepository;
    private final AgentAssuranceRepository agentRepository;
    private final InsuranceProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ClientScoringService clientScoringService;
    private final ClaimRepository claimRepository;
    private final PaymentRepository paymentRepository;

    // ============================================================
    // 🔥 ADD CONTRACT
    // ============================================================

    @Override
    @Transactional
    public InsuranceContractDTO addContract(InsuranceContractDTO dto, String userEmail) {
        // 1️⃣ Récupérer le client connecté
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!(user instanceof Client)) {
            throw new AccessDeniedException("Seuls les clients peuvent créer des contrats");
        }
        Client client = (Client) user;

        // 2️⃣ Vérifier que le client a un agent d'assurance assigné
        if (client.getAgentAssurance() == null) {
            throw new RuntimeException("Vous devez avoir un agent d'assurance assigné pour créer un contrat");
        }

        // 3️⃣ Création du contrat
        InsuranceContract contract = InsuranceContractMapper.toEntity(dto);
        contract.setClient(client);
        contract.setAgentAssurance(client.getAgentAssurance());

        InsuranceProduct product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        contract.setProduct(product);

        contract.setPaymentFrequency(Enum.valueOf(PaymentFrequency.class, dto.getPaymentFrequency()));

        // 4️⃣ Calcul avancé du coverageLimit
        double coverageLimit = determineAdvancedCoverageLimit(contract);
        System.out.println("💡 Coverage Limit avancé fixé à : " + coverageLimit);

        // 5️⃣ Ajuster la prime automatiquement selon le coverageLimit
        double basePremium = dto.getPremium() != null ? dto.getPremium() : coverageLimit * 0.02; // 2% du coverage
        double finalPremium = basePremium * 1.10; // +10% frais/ajustement
        contract.setPremium(finalPremium);
        contract.setTotalPaid(0.0);
        contract.setRemainingAmount(finalPremium);

        // 6️⃣ Déductible = 10% de la prime
        contract.setDeductible(finalPremium * 0.10);

        // 7️⃣ Calcul du risque
        RiskClaim riskClaim = calculateRisk(contract); // ta méthode existante
        riskClaim.setContract(contract);
        contract.setRiskClaim(riskClaim);

        // 8️⃣ Statut initial: INACTIF ou annulé si risque HIGH
        if ("HIGH".equalsIgnoreCase(riskClaim.getRiskLevel())) {
            contract.setStatus(ContractStatus.CANCELLED);
        } else {
            contract.setStatus(ContractStatus.INACTIVE);
        }

        // 9️⃣ Génération des paiements planifiés selon le montant final et la fréquence
        contract.setPayments(new ArrayList<>());
        generateScheduledPayments(contract); // ta méthode existante

        // 🔟 Sauvegarde finale
        contract = contractRepository.save(contract);

        return InsuranceContractMapper.toDTO(contract);
    }

    public double determineAdvancedCoverageLimit(InsuranceContract contract) {
        if (contract.getProduct() == null || contract.getClient() == null)
            throw new RuntimeException("Produit ou client non sélectionné");

        double baseLimit = contract.getProduct().getBasePrice();
        double clientIncome = contract.getClient().getAnnualIncome() != null
                ? contract.getClient().getAnnualIncome()
                : 0.0;

        int clientAge = contract.getClient().getAge();
        boolean hasClaimsHistory = contract.getClient().getClaims() != null && !contract.getClient().getClaims().isEmpty();

        // Facteurs pondérés
        double factorProduct = 1.5;
        double factorIncome = 0.3;
        double factorAge = clientAge > 50 ? 1.2 : 1.0;
        double factorClaims = hasClaimsHistory ? 0.8 : 1.0;

        // Calcul du coverage limit
        double coverageLimit = (baseLimit * factorProduct + clientIncome * factorIncome) * factorAge * factorClaims;

        // Plafond et plancher
        double minLimit = baseLimit;
        double maxLimit = baseLimit * 10;
        coverageLimit = Math.max(Math.min(coverageLimit, maxLimit), minLimit);

        contract.setCoverageLimit(coverageLimit);
        return coverageLimit;
    }

    @Override
    @Transactional
    public InsuranceContractDTO activateContract(Long contractId, String agentEmail) {
        log.info("🔧 Tentative d'activation du contrat {} par l'agent {}", contractId, agentEmail);

        // 1. Vérifier que c'est bien un agent
        User user = userRepository.findByEmail(agentEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!(user instanceof AgentAssurance)) {
            throw new AccessDeniedException("Seuls les agents d'assurance peuvent activer des contrats");
        }

        AgentAssurance agent = (AgentAssurance) user;
        log.info("✅ Agent authentifié: {} {}", agent.getFirstName(), agent.getLastName());

        // 2. Récupérer le contrat
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        log.info("📄 Contrat trouvé: ID={}, Statut={}, Client={}",
                contract.getContractId(), contract.getStatus(),
                contract.getClient().getEmail());

        // 3. Vérifier que le contrat appartient bien à un client de cet agent
        if (!contract.getClient().getAgentAssurance().getId().equals(agent.getId())) {
            log.error("❌ Contrat {} n'appartient pas à l'agent {}", contractId, agent.getId());
            throw new AccessDeniedException("Ce contrat n'appartient pas à un de vos clients");
        }

        // 4. Vérifier que le contrat est INACTIVE
        if (contract.getStatus() != ContractStatus.INACTIVE) {
            log.error("❌ Contrat {} n'est pas INACTIVE (statut: {})", contractId, contract.getStatus());
            throw new RuntimeException("Seuls les contrats INACTIVE peuvent être activés");
        }

        // 5. Vérifier le niveau de risque
        if (contract.getRiskClaim() != null && "HIGH".equals(contract.getRiskClaim().getRiskLevel())) {
            log.error("❌ Contrat {} a un risque HIGH, activation impossible", contractId);
            throw new RuntimeException("Impossible d'activer un contrat à risque HIGH");
        }

        // 6. Activer le contrat
        contract.setStatus(ContractStatus.ACTIVE);
        contract = contractRepository.save(contract);
        log.info("✅ Contrat {} activé avec succès", contractId);

        // 7. Envoyer un email de confirmation au client
        Client client = contract.getClient();
        try {
            emailService.sendContractAcceptedEmail(client, contract);
            log.info("✅ Email de confirmation envoyé à {} pour le contrat {}",
                    client.getEmail(), contract.getContractId());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de confirmation: {}", e.getMessage());
            // Ne pas bloquer l'activation même si l'email échoue
        }

        return InsuranceContractMapper.toDTO(contract);
    }

    // 🔥 NOUVELLE MÉTHODE : Activer et envoyer email en une seule opération
    @Transactional
    public InsuranceContractDTO activateAndNotify(Long contractId, String agentEmail) {
        InsuranceContractDTO activatedContract = activateContract(contractId, agentEmail);
        // L'email est déjà envoyé dans activateContract
        return activatedContract;
    }

    @Override
    public List<InsuranceContractDTO> getAllContracts(String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // ================= CLIENT =================
        if (user instanceof Client client) {

            return contractRepository.findByClient(client)
                    .stream()
                    .map(InsuranceContractMapper::toDTO)
                    .collect(Collectors.toList());
        }

        // ================= AGENT =================
        if (user instanceof AgentAssurance agent) {

            return contractRepository.findByAgentAssuranceId(agent.getId())
                    .stream()
                    .map(InsuranceContractMapper::toDTO)
                    .collect(Collectors.toList());
        }

        // ================= ADMIN =================
        return contractRepository.findAll()
                .stream()
                .map(InsuranceContractMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public InsuranceContractDTO getContractById(Long id, String userEmail) {
        InsuranceContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérification des droits d'accès
        if (user instanceof Client) {
            // Un client ne voit que ses propres contrats
            if (!contract.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous n'avez pas accès à ce contrat");
            }
        } else if (user instanceof AgentAssurance) {
            // Un agent ne voit que les contrats de ses clients
            AgentAssurance agent = (AgentAssurance) user;
            if (!contract.getClient().getAgentAssurance().getId().equals(agent.getId())) {
                throw new AccessDeniedException("Ce contrat n'appartient pas à un de vos clients");
            }
        }
        // Admin peut tout voir

        return InsuranceContractMapper.toDTO(contract);
    }

    @Override
    @Transactional
    public InsuranceContractDTO updateContract(Long contractId, InsuranceContractDTO dto, String userEmail) {
        if (contractId == null) {
            throw new IllegalArgumentException("L'id du contrat ne peut pas être null");
        }

        // 1️⃣ Récupérer le contrat
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé avec id: " + contractId));

        // 2️⃣ Récupérer l'utilisateur
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 3️⃣ Vérification d'autorisation
        if (user instanceof Client clientUser) {
            if (contract.getClient() == null || !contract.getClient().getId().equals(clientUser.getId())) {
                throw new AccessDeniedException("Vous ne pouvez modifier que vos propres contrats");
            }
        } else if (!(user instanceof Admin) && !(user instanceof AgentAssurance)) {
            throw new AccessDeniedException("Modification non autorisée");
        }

        // 4️⃣ Mise à jour des champs de base (uniquement si non null)
        if (dto.getStartDate() != null) contract.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) contract.setEndDate(dto.getEndDate());
        if (dto.getPremium() != null && dto.getPremium() > 0) contract.setPremium(dto.getPremium());
        if (dto.getDeductible() != null && dto.getDeductible() > 0) contract.setDeductible(dto.getDeductible());
        if (dto.getCoverageLimit() != null && dto.getCoverageLimit() > 0) contract.setCoverageLimit(dto.getCoverageLimit());

        // 5️⃣ Mise à jour du montant restant
        contract.setRemainingAmount(contract.getPremium() - contract.getTotalPaid());

        // 6️⃣ Mise à jour du statut automatique
        if (contract.getRemainingAmount() <= 0) {
            contract.setStatus(ContractStatus.COMPLETED);
        } else if (dto.getStatus() != null) {
            contract.setStatus(Enum.valueOf(ContractStatus.class, dto.getStatus()));
        }

        // 7️⃣ Mise à jour de la fréquence de paiement
        // 7️⃣ Mise à jour de la fréquence de paiement
        if (dto.getPaymentFrequency() != null) {
            PaymentFrequency newFrequency = Enum.valueOf(PaymentFrequency.class, dto.getPaymentFrequency());

            if (contract.getPaymentFrequency() != newFrequency) {
                // Compter les paiements déjà payés
                long paidCount = contract.getPayments().stream()
                        .filter(p -> p.getStatus() == PaymentStatus.PAID)
                        .count();

                if (paidCount > 0) {
                    throw new RuntimeException(
                            "Impossible de changer la fréquence après que des paiements aient été effectués");
                }

                // Supprimer tous les paiements existants
                contract.getPayments().clear();
                paymentRepository.deleteByContract_ContractId(contract.getContractId()); // si tu as un repository

                // Appliquer la nouvelle fréquence
                contract.setPaymentFrequency(newFrequency);

                // Recalculer remainingAmount
                double totalPaid = contract.getTotalPaid();
                contract.setRemainingAmount(Math.max(contract.getPremium() - totalPaid, 0));

                // Générer de nouveaux paiements selon la nouvelle fréquence
                regenerateScheduledPayments(contract);
            }
        }

        // 8️⃣ Mise à jour des relations (client, agent, produit)
        if (dto.getClientId() != null) {
            Client client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));
            contract.setClient(client);
        }

        if (dto.getAgentAssuranceId() != null) {
            AgentAssurance agent = agentRepository.findById(dto.getAgentAssuranceId())
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));
            contract.setAgentAssurance(agent);
        }

        if (dto.getProductId() != null) {
            InsuranceProduct product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
            contract.setProduct(product);
        }

        // 9️⃣ Recalcul du risque
        RiskClaim existingRisk = contract.getRiskClaim();
        if (existingRisk == null) {
            existingRisk = calculateRisk(contract);
            existingRisk.setContract(contract);
            contract.setRiskClaim(existingRisk);
        } else {
            RiskClaim updatedRisk = calculateRisk(contract);
            existingRisk.setRiskScore(updatedRisk.getRiskScore());
            existingRisk.setRiskLevel(updatedRisk.getRiskLevel());
            existingRisk.setEvaluationNote(updatedRisk.getEvaluationNote());
        }

        // 🔟 Annuler automatiquement si risque HIGH
        if ("HIGH".equals(contract.getRiskClaim().getRiskLevel())) {
            contract.setStatus(ContractStatus.CANCELLED);
        }

        // 1️⃣1️⃣ Sauvegarde finale
        contract = contractRepository.save(contract);

        return InsuranceContractMapper.toDTO(contract);
    }

    @Override
    @Transactional
    public void deleteContract(Long id, String userEmail) {
        InsuranceContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérification: seul le propriétaire ou admin peut supprimer
        if (user instanceof Client) {
            if (!contract.getClient().getId().equals(user.getId())) {
                throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres contrats");
            }
        } else if (!(user instanceof Admin)) {
            throw new AccessDeniedException("Suppression non autorisée");
        }

        contractRepository.deleteById(id);
    }

    // ============================================================
    // 🔥 GÉNÉRATION DES PAIEMENTS
    // ============================================================

    private void generateScheduledPayments(InsuranceContract contract) {
        if (contract.getPaymentFrequency() == null) return;

        Date start = contract.getStartDate();
        Date end = contract.getEndDate();

        if (start == null || end == null) {
            log.warn("Dates de contrat manquantes pour le contrat {}", contract.getContractId());
            return;
        }

        // Calculer la durée en années
        long durationInMillis = end.getTime() - start.getTime();
        long durationInYears = durationInMillis / (1000L * 60 * 60 * 24 * 365);

        // Éviter la division par zéro
        if (durationInYears < 1) {
            durationInYears = 1;
        }

        contract.setContractDurationYears((int) durationInYears);

        double installment = contract.calculateInstallmentAmount();

        // Arrondir à 2 décimales
        installment = Math.round(installment * 100.0) / 100.0;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);

        int paymentCount = 0;
        int maxPayments = getMaxPayments(contract.getPaymentFrequency(), (int) durationInYears);

        log.info("📊 Génération de {} paiements de {} DT pour le contrat {} (durée: {} ans, fréquence: {})",
                maxPayments, installment, contract.getContractId(), durationInYears, contract.getPaymentFrequency());

        while (paymentCount < maxPayments) {
            Payment payment = new Payment();
            payment.setContract(contract);

            // Pour le dernier paiement, ajuster pour éviter les erreurs d'arrondi
            if (paymentCount == maxPayments - 1) {
                double totalSoFar = installment * paymentCount;
                payment.setAmount(Math.max(0, contract.getPremium() - totalSoFar));
            } else {
                payment.setAmount(installment);
            }

            payment.setPaymentDate(calendar.getTime());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

            contract.getPayments().add(payment);
            paymentCount++;

            switch (contract.getPaymentFrequency()) {
                case MONTHLY -> calendar.add(Calendar.MONTH, 1);
                case SEMI_ANNUAL -> calendar.add(Calendar.MONTH, 6);
                case ANNUAL -> calendar.add(Calendar.YEAR, 1);
            }
        }

        log.info("✅ {} paiements générés pour le contrat {}", contract.getPayments().size(), contract.getContractId());
    }

    private int getMaxPayments(PaymentFrequency frequency, int durationYears) {
        switch (frequency) {
            case MONTHLY: return durationYears * 12;
            case SEMI_ANNUAL: return durationYears * 2;
            case ANNUAL: return durationYears;
            default: return 1;
        }
    }

    private void regenerateScheduledPayments(InsuranceContract contract) {
        if (contract.getPayments() == null) {
            contract.setPayments(new ArrayList<>());
            return;
        }

        // Supprimer uniquement les paiements en attente
        List<Payment> toRemove = contract.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .toList();

        contract.getPayments().removeAll(toRemove);

        // Supprimer aussi de la base de données
        if (!toRemove.isEmpty()) {
            paymentRepository.deleteAll(toRemove);
        }

        generateScheduledPayments(contract);
    }

    // ============================================================
// 🔥 CALCUL DU RISQUE - VERSION PROFESSIONNELLE AVEC SCORING CLIENT
// ============================================================

    private RiskClaim calculateRisk(InsuranceContract contract) {
        RiskClaim riskClaim = new RiskClaim();
        riskClaim.setContract(contract);

        double score = 0;
        StringBuilder evaluation = new StringBuilder();
        List<String> riskFactors = new ArrayList<>();

        // ============================================================
        // 1. SCORING CLIENT (30% du poids total)
        // ============================================================

        Client client = contract.getClient();
        ClientScoreResult clientScore = clientScoringService.calculateClientScore(client.getId());

        evaluation.append("📊 **ANALYSE CLIENT**\n");
        evaluation.append("==================\n");
        evaluation.append(String.format("Score Client: %.2f/100\n", clientScore.getGlobalScore()));
        evaluation.append(String.format("Niveau de risque: %s\n", clientScore.getRiskLevel()));
        evaluation.append(String.format("Classe de risque: %s\n", clientScore.getRiskClass()));
        evaluation.append("\nDétails par composante:\n");

        // Afficher les scores par composante
        clientScore.getComponentScores().forEach((key, value) -> {
            evaluation.append(String.format("- %s: %.2f\n", key, value));
        });

        // Ajouter les recommandations si existantes
        if (!clientScore.getRecommendations().isEmpty()) {
            evaluation.append("\nRecommandations client:\n");
            clientScore.getRecommendations().forEach((key, value) -> {
                evaluation.append(String.format("- %s\n", value));
            });
        }

        // Intégration du score client dans le risque global
        double clientRiskContribution;
        if (clientScore.getRiskLevel().equals("TRES_ELEVE")) {
            clientRiskContribution = 40;
            riskFactors.add("Client à risque très élevé (score < 35)");
        } else if (clientScore.getRiskLevel().equals("ELEVE")) {
            clientRiskContribution = 30;
            riskFactors.add("Client à risque élevé (score 35-50)");
        } else if (clientScore.getRiskLevel().equals("MODERE")) {
            clientRiskContribution = 20;
            riskFactors.add("Client à risque modéré (score 50-65)");
        } else if (clientScore.getRiskLevel().equals("FAIBLE")) {
            clientRiskContribution = 10;
            riskFactors.add("Client à risque faible (score 65-80)");
        } else {
            clientRiskContribution = 5;
            riskFactors.add("Client à risque très faible (score > 80)");
        }

        score += clientRiskContribution;
        evaluation.append(String.format("\nContribution client au risque: +%.0f points\n\n", clientRiskContribution));

        // ============================================================
        // 2. ANALYSE DU PRODUIT (20% du poids)
        // ============================================================

        evaluation.append("📦 **ANALYSE DU PRODUIT**\n");
        evaluation.append("=====================\n");

        InsuranceProduct product = contract.getProduct();
        if (product != null) {
            ProductType productType = product.getProductType(); // enum

            // Pour l'affichage
            evaluation.append(String.format("Type de produit: %s\n", productType.name()));

            // Comparaison avec l'enum
            if (productType == ProductType.LIFE) {
                score += 20;
                riskFactors.add("Produit d'assurance vie - risque élevé");
                evaluation.append("⚠️ Produit vie: +20 points (risque actuariel élevé)\n");
            } else if (productType == ProductType.HEALTH) {
                score += 15;
                riskFactors.add("Produit santé - risque modéré");
                evaluation.append("⚕️ Produit santé: +15 points\n");
            } else if (productType == ProductType.AUTO) {
                score += 10;
                riskFactors.add("Produit auto - risque standard");
                evaluation.append("🚗 Produit auto: +10 points\n");
            } else if (productType == ProductType.HOME) {
                score += 8;
                evaluation.append("🏠 Produit habitation: +8 points\n");
            } else if (productType == ProductType.OTHER) {
                score += 5; // ou selon ta logique
                evaluation.append("📦 Produit autre: +5 points\n");
            }
        }

        evaluation.append("\n");

        // ============================================================
        // 3. ANALYSE FINANCIÈRE DU CONTRAT (30% du poids)
        // ============================================================

        evaluation.append("💰 **ANALYSE FINANCIÈRE**\n");
        evaluation.append("======================\n");

        // Ratio prime/revenus
        if (client.getAnnualIncome() != null && client.getAnnualIncome() > 0) {
            double premiumToIncomeRatio = (contract.getPremium() / client.getAnnualIncome()) * 100;
            evaluation.append(String.format("Ratio prime/revenus: %.2f%%\n", premiumToIncomeRatio));

            if (premiumToIncomeRatio > 30) {
                score += 25;
                riskFactors.add("Prime excessive (>30% des revenus)");
                evaluation.append("⚠️ Prime très élevée par rapport aux revenus: +25 points\n");
            } else if (premiumToIncomeRatio > 20) {
                score += 15;
                riskFactors.add("Prime élevée (20-30% des revenus)");
                evaluation.append("⚠️ Prime élevée par rapport aux revenus: +15 points\n");
            } else if (premiumToIncomeRatio > 10) {
                score += 8;
                evaluation.append("📊 Prime modérée par rapport aux revenus: +8 points\n");
            } else {
                evaluation.append("✅ Prime adaptée aux revenus: +0 points\n");
            }
        }

        // Analyse de la franchise
        evaluation.append(String.format("\nFranchise: %.2f DT\n", contract.getDeductible()));
        if (contract.getDeductible() < 200) {
            score += 20;
            riskFactors.add("Franchise trop basse (<200 DT)");
            evaluation.append("⚠️ Franchise très basse: +20 points\n");
        } else if (contract.getDeductible() < 500) {
            score += 10;
            evaluation.append("📊 Franchise standard: +10 points\n");
        } else {
            evaluation.append("✅ Franchise élevée (sécurisante): +5 points\n");
        }

        // Analyse du plafond
        evaluation.append(String.format("\nPlafond de couverture: %.2f DT\n", contract.getCoverageLimit()));
        if (contract.getCoverageLimit() > 200000) {
            score += 25;
            riskFactors.add("Plafond très élevé (>200k DT)");
            evaluation.append("⚠️ Plafond très élevé: +25 points\n");
        } else if (contract.getCoverageLimit() > 100000) {
            score += 15;
            evaluation.append("📊 Plafond élevé: +15 points\n");
        } else if (contract.getCoverageLimit() > 50000) {
            score += 8;
            evaluation.append("✅ Plafond modéré: +8 points\n");
        } else {
            evaluation.append("✅ Plafond standard: +5 points\n");
        }

        evaluation.append("\n");

        // ============================================================
        // 4. ANALYSE TEMPORELLE (20% du poids)
        // ============================================================

        evaluation.append("⏱️ **ANALYSE TEMPORELLE**\n");
        evaluation.append("=====================\n");

        if (contract.getStartDate() != null && contract.getEndDate() != null) {
            long durationInDays = (contract.getEndDate().getTime() - contract.getStartDate().getTime()) / (1000 * 60 * 60 * 24);
            long durationInYears = durationInDays / 365;

            evaluation.append(String.format("Durée du contrat: %d ans (%d jours)\n", durationInYears, durationInDays));

            if (durationInYears > 10) {
                score += 25;
                riskFactors.add("Durée très longue (>10 ans)");
                evaluation.append("⚠️ Durée très longue: +25 points\n");
            } else if (durationInYears > 5) {
                score += 15;
                evaluation.append("📊 Durée longue: +15 points\n");
            } else if (durationInYears > 3) {
                score += 10;
                evaluation.append("✅ Durée moyenne: +10 points\n");
            } else {
                evaluation.append("✅ Durée courte: +5 points\n");
            }

            // Vérifier si le client est âgé par rapport à la durée
            int clientAge = client.getAge();
            int ageAtEnd = clientAge + (int) durationInYears;

            if (ageAtEnd > 75) {
                score += 15;
                riskFactors.add(String.format("Client âgé de %d ans en fin de contrat", ageAtEnd));
                evaluation.append(String.format("⚠️ Client âgé de %d ans en fin de contrat: +15 points\n", ageAtEnd));
            }
        }

        evaluation.append("\n");

        // ============================================================
        // 5. BONUS/MALUS SPÉCIFIQUES
        // ============================================================

        evaluation.append("🎯 **BONUS/MALUS**\n");
        evaluation.append("===============\n");

        // Malus pour cumul de contrats
        int existingContracts = client.getContracts() != null ? client.getContracts().size() : 0;
        if (existingContracts > 3) {
            score += 10;
            riskFactors.add("Cumul de contrats (>3)");
            evaluation.append("⚠️ Cumul de contrats: +10 points\n");
        }

        // Bonus pour client fidèle
        if (client.getClientTenureInDays() > 365 * 5) {
            score -= 5; // Bonus = réduction du score de risque
            evaluation.append("✅ Client fidèle (>5 ans): -5 points (bonus)\n");
        }

        // Malus pour sinistres antérieurs
        List<Claim> clientClaims = claimRepository.findByClientId(client.getId());
        if (clientClaims != null && !clientClaims.isEmpty()) {
            long approvedClaims = clientClaims.stream()
                    .filter(c -> c.getStatus() == ClaimStatus.APPROVED)
                    .count();
            if (approvedClaims > 2) {
                score += 15;
                riskFactors.add("Sinistres répétés");
                evaluation.append("⚠️ Sinistres répétés: +15 points\n");
            }
        }

        evaluation.append("\n");

        // ============================================================
        // 6. SCORE FINAL ET NIVEAU DE RISQUE
        // ============================================================

        // Normalisation du score (max 100)
        double finalScore = Math.min(100, score);
        riskClaim.setRiskScore(finalScore);

        evaluation.append("📋 **RÉSULTAT FINAL**\n");
        evaluation.append("=================\n");
        evaluation.append(String.format("Score total: %.2f/100\n", finalScore));

        // Facteurs de risque
        if (!riskFactors.isEmpty()) {
            evaluation.append("\n🔴 Facteurs de risque identifiés:\n");
            riskFactors.forEach(factor -> evaluation.append("- ").append(factor).append("\n"));
        }

        // Recommandations du scoring client
        if (!clientScore.getRecommendations().isEmpty()) {
            evaluation.append("\n💡 Recommandations du scoring client:\n");
            clientScore.getRecommendations().values()
                    .forEach(rec -> evaluation.append("- ").append(rec).append("\n"));
        }

        // Déterminer le niveau de risque final
        String riskLevel;
        String recommendation;
        boolean autoReject;

        if (finalScore >= 80) {
            riskLevel = "HIGH";
            autoReject = true;
            recommendation = "🔴 RISQUE TRÈS ÉLEVÉ - Contrat automatiquement rejeté. " +
                    "Score trop élevé et/ou client à risque.";
            evaluation.insert(0, "🔴 **RISQUE ÉLEVÉ - REJET AUTO**\n\n");
        } else if (finalScore >= 60) {
            riskLevel = "MEDIUM";
            autoReject = false;
            recommendation = "🟡 RISQUE MOYEN - Nécessite validation par un agent. " +
                    "Vérifier les points d'attention identifiés.";
            evaluation.insert(0, "🟡 **RISQUE MOYEN - REVISION NÉCESSAIRE**\n\n");
        } else if (finalScore >= 40) {
            riskLevel = "LOW";
            autoReject = false;
            recommendation = "🟢 RISQUE FAIBLE - Peut être accepté. " +
                    "Surveillance standard recommandée.";
            evaluation.insert(0, "🟢 **RISQUE FAIBLE - ACCEPTABLE**\n\n");
        } else {
            riskLevel = "VERY_LOW";
            autoReject = false;
            recommendation = "✅ RISQUE TRÈS FAIBLE - Acceptation automatique recommandée.";
            evaluation.insert(0, "✅ **RISQUE TRÈS FAIBLE - FAVORABLE**\n\n");
        }

        evaluation.append("\n💡 **RECOMMANDATION**: ").append(recommendation);

        riskClaim.setRiskLevel(riskLevel);
        riskClaim.setEvaluationNote(evaluation.toString());

        // Log pour le suivi
        log.info("📊 Calcul de risque pour contrat {}: score={}, niveau={}, autoReject={}",
                contract.getContractId(), finalScore, riskLevel, autoReject);

        // Si autoReject est true, on notifie
        if (autoReject) {
            log.warn("🚨 Contrat {} automatiquement rejeté - Score: {}, Niveau: {}",
                    contract.getContractId(), finalScore, riskLevel);
        }

        return riskClaim;
    }

    // ============================================================
    // 🔥 VÉRIFICATION DES RETARDS DE PAIEMENT
    // ============================================================

    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void checkLatePayments() {
        log.info("🔍 Vérification quotidienne des retards - {}", new Date());

        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        log.info("📊 {} contrat(s) actif(s)", activeContracts.size());

        int contractsCancelled = 0;
        int totalLatePayments = 0;

        for (InsuranceContract contract : activeContracts) {
            // Marquer les nouveaux retards
            checkAndMarkLatePaymentsByFrequency(contract);

            // Compter les retards avant annulation
            int beforeLateCount = countLatePayments(contract);

            // Vérifier si le contrat doit être annulé (≥ 4 retards)
            ContractStatus beforeStatus = contract.getStatus();
            checkAndCancelContractForLatePayments(contract);
            ContractStatus afterStatus = contract.getStatus();

            // Compter les retards après traitement
            int afterLateCount = countLatePayments(contract);

            if (beforeStatus != afterStatus && afterStatus == ContractStatus.CANCELLED) {
                contractsCancelled++;
                log.warn("🚨 Contrat {} annulé (avait {} retards)", contract.getContractId(), afterLateCount);
            }

            totalLatePayments += afterLateCount;
        }

        // Vérifier aussi les contrats INACTIVE
        List<InsuranceContract> inactiveContracts = contractRepository.findByStatus(ContractStatus.INACTIVE);
        for (InsuranceContract contract : inactiveContracts) {
            checkAndMarkLatePaymentsByFrequency(contract);
        }

        log.info("📊 Résumé: {} contrat(s) annulé(s), {} total paiements en retard",
                contractsCancelled, totalLatePayments);
    }

    /**
     * Compter le nombre de paiements en retard pour un contrat
     */
    private int countLatePayments(InsuranceContract contract) {
        if (contract.getPayments() == null) return 0;
        return (int) contract.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.LATE)
                .count();
    }

    @Override
    @Scheduled(cron = "0 59 23 L * ?")
    @Transactional
    public void checkEndOfMonthLatePayments() {
        System.out.println("📅 Vérification de fin de mois - " + new Date());

        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        for (InsuranceContract contract : activeContracts) {
            checkAndMarkMonthlyLatePayments(contract);
            checkAndCancelContractForLatePayments(contract);
        }

        List<InsuranceContract> inactiveContracts = contractRepository.findByStatus(ContractStatus.INACTIVE);
        for (InsuranceContract contract : inactiveContracts) {
            checkAndMarkMonthlyLatePayments(contract);
        }
    }

    @Override
    public void checkContractLatePayments(Long contractId) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        System.out.println("🔍 Vérification manuelle du contrat " + contractId);
        checkAndMarkLatePaymentsByFrequency(contract);
        checkAndCancelContractForLatePayments(contract);
    }

    @Override
    public void simulateLatePayments(Long contractId, int monthsToAdd) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        Calendar cal = Calendar.getInstance();
        for (Payment payment : contract.getPayments()) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                cal.setTime(payment.getPaymentDate());
                cal.add(Calendar.MONTH, -monthsToAdd);
                payment.setPaymentDate(cal.getTime());
            }
        }

        contractRepository.save(contract);
        System.out.println("⏱️ Simulation: " + monthsToAdd + " mois de retard - contrat " + contractId);
    }

    @Override
    @Scheduled(cron = "0 30 0 * * ?")
    @Transactional
    public void checkCompletedContracts() {
        System.out.println("✅ Vérification des contrats à marquer COMPLETED - " + new Date());

        List<InsuranceContract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        for (InsuranceContract contract : activeContracts) {
            checkAndMarkContractAsCompleted(contract);
        }

        List<InsuranceContract> inactiveContracts = contractRepository.findByStatus(ContractStatus.INACTIVE);
        for (InsuranceContract contract : inactiveContracts) {
            checkAndMarkContractAsCompleted(contract);
        }
    }

    // ============================================================
    // 🔥 MÉTHODES PRIVÉES
    // ============================================================

    private void checkAndMarkLatePaymentsByFrequency(InsuranceContract contract) {
        if (contract.getPayments() == null || contract.getPayments().isEmpty()) return;

        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        boolean paymentsUpdated = false;

        for (Payment payment : contract.getPayments()) {
            if (payment.getStatus() != PaymentStatus.PENDING) continue;

            Date paymentDate = payment.getPaymentDate();
            cal.setTime(paymentDate);

            switch (contract.getPaymentFrequency()) {
                case MONTHLY:
                    cal.add(Calendar.MONTH, 1);
                    break;
                case SEMI_ANNUAL:
                    cal.add(Calendar.MONTH, 6);
                    break;
                case ANNUAL:
                    cal.add(Calendar.YEAR, 1);
                    break;
            }

            if (cal.getTime().before(today)) {
                payment.setStatus(PaymentStatus.LATE);
                paymentsUpdated = true;
                System.out.println("⏰ Paiement " + payment.getPaymentId() + " marqué LATE");
            }
        }

        if (paymentsUpdated) contractRepository.save(contract);
    }

    private void checkAndMarkMonthlyLatePayments(InsuranceContract contract) {
        if (contract.getPayments() == null || contract.getPayments().isEmpty()) return;

        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date firstDayOfMonth = cal.getTime();

        boolean paymentsUpdated = false;

        for (Payment payment : contract.getPayments()) {
            if (payment.getStatus() == PaymentStatus.PENDING &&
                    payment.getPaymentDate().before(firstDayOfMonth) &&
                    contract.getPaymentFrequency() == PaymentFrequency.MONTHLY) {

                payment.setStatus(PaymentStatus.LATE);
                paymentsUpdated = true;
                System.out.println("📆 Paiement mensuel " + payment.getPaymentId() + " marqué LATE");
            }
        }

        if (paymentsUpdated) contractRepository.save(contract);
    }

    private void checkAndCancelContractForLatePayments(InsuranceContract contract) {
        if (contract.getPayments() == null || contract.getPayments().isEmpty()) return;

        int latePaymentCount = 0;
        int pendingPaymentCount = 0;
        boolean paymentsUpdated = false;

        // Compter les paiements en retard et en attente
        for (Payment payment : contract.getPayments()) {
            if (payment.getStatus() == PaymentStatus.LATE) {
                latePaymentCount++;
            } else if (payment.getStatus() == PaymentStatus.PENDING) {
                pendingPaymentCount++;
            }
        }

        // 🚨 NOUVEAU SEUIL: 4 paiements en retard pour annulation
        if (latePaymentCount >= 4) {
            log.warn("🚨 CONTRAT {} ANNULÉ - {} paiements en retard (seuil: 4)",
                    contract.getContractId(), latePaymentCount);

            // Changer le statut du contrat
            contract.setStatus(ContractStatus.CANCELLED);

            // Marquer tous les paiements en attente comme FAILED
            for (Payment payment : contract.getPayments()) {
                if (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.LATE) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentsUpdated = true;
                }
            }

            // 📧 Envoyer un email d'annulation au client
            Client client = contract.getClient();
            if (client != null && client.getEmail() != null) {
                try {
                    emailService.sendContractCancelledEmail(client, contract, latePaymentCount);
                    log.info("📧 Email d'annulation envoyé à {} pour le contrat {}",
                            client.getEmail(), contract.getContractId());
                } catch (Exception e) {
                    log.error("❌ Erreur lors de l'envoi de l'email d'annulation: {}", e.getMessage());
                }
            }

            log.info("📊 Statistiques - Contrat {}: {} paiements en retard, {} paiements en attente marqués FAILED",
                    contract.getContractId(), latePaymentCount, pendingPaymentCount);

        } else if (latePaymentCount > 0) {
            // Simple information si des retards existent mais pas encore 4
            log.info("ℹ️ Contrat {}: {} paiements en retard (seuil non atteint)",
                    contract.getContractId(), latePaymentCount);
        }

        if (paymentsUpdated || latePaymentCount >= 4) {
            contractRepository.save(contract);
        }
    }

    private void checkAndMarkContractAsCompleted(InsuranceContract contract) {
        if (contract.getPayments() == null || contract.getPayments().isEmpty()) {
            return;
        }

        Date today = new Date();
        Date endDate = contract.getEndDate();

        boolean isEndDatePassed = endDate != null && endDate.before(today);

        if (!isEndDatePassed) {
            return;
        }

        boolean allPaymentsPaid = true;
        int totalPayments = 0;
        int paidPayments = 0;

        for (Payment payment : contract.getPayments()) {
            totalPayments++;
            if (payment.getStatus() == PaymentStatus.PAID) {
                paidPayments++;
            } else {
                allPaymentsPaid = false;
            }
        }

        boolean totalPaidMatches = Math.abs(contract.getTotalPaid() - contract.getPremium()) < 0.01;

        if (allPaymentsPaid && totalPaidMatches) {
            contract.setStatus(ContractStatus.COMPLETED);
            contractRepository.save(contract);
            System.out.println("🎉 CONTRAT " + contract.getContractId() + " MARQUÉ COMPLETED");
        }
    }

    public List<InsuranceContractDTO> getContractsByClientEmail(String email) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        return contractRepository.findByClient(client)
                .stream()
                .map(InsuranceContractMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public InsuranceContractDTO rejectContract(Long contractId, String agentEmail, String rejectionReason) {
        // 1. Vérifier que c'est bien un agent
        User user = userRepository.findByEmail(agentEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!(user instanceof AgentAssurance)) {
            throw new AccessDeniedException("Seuls les agents d'assurance peuvent rejeter des contrats");
        }

        AgentAssurance agent = (AgentAssurance) user;

        // 2. Récupérer le contrat
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        // 3. Vérifier que le contrat appartient bien à un client de cet agent
        if (!contract.getClient().getAgentAssurance().getId().equals(agent.getId())) {
            throw new AccessDeniedException("Ce contrat n'appartient pas à un de vos clients");
        }

        // 4. Vérifier que le contrat est INACTIVE
        if (contract.getStatus() != ContractStatus.INACTIVE) {
            throw new RuntimeException("Seuls les contrats INACTIVE peuvent être rejetés");
        }

        // 5. Rejeter le contrat en utilisant CANCELLED
        contract.setStatus(ContractStatus.CANCELLED);
        contract = contractRepository.save(contract);

        // 📧 6. Envoyer un email de notification au client
        Client client = contract.getClient();
        try {
            emailService.sendContractRejectedEmail(client, contract, rejectionReason);
            log.info("✅ Email de rejet envoyé à {} pour le contrat {}",
                    client.getEmail(), contract.getContractId());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de rejet: {}", e.getMessage());
            // Ne pas bloquer le rejet même si l'email échoue
        }

        // Log pour traçabilité
        log.info("❌ Contrat {} rejeté par l'agent {} - Raison: {}",
                contractId, agentEmail, rejectionReason);

        return InsuranceContractMapper.toDTO(contract);
    }

    @Scheduled(cron = "0 */05 * * * ?")
    @Transactional
    public void updateContractsPaymentStatus() {

        System.out.println("🔄 Vérification automatique des paiements - " + new Date());

        List<InsuranceContract> contracts = contractRepository.findAll();

        for (InsuranceContract contract : contracts) {

            if (contract.getPayments() == null || contract.getPayments().isEmpty())
                continue;

            double totalPaid = 0.0;

            // 🔎 Calculer la somme des paiements PAYÉS
            for (Payment payment : contract.getPayments()) {
                if (payment.getStatus() == PaymentStatus.PAID) {
                    totalPaid += payment.getAmount();
                }
            }

            // 🔄 Mettre à jour les montants
            contract.setTotalPaid(totalPaid);
            contract.setRemainingAmount(contract.getPremium() - totalPaid);

            // 🔥 Mise à jour automatique du statut
            if (contract.getRemainingAmount() <= 0) {
                contract.setStatus(ContractStatus.COMPLETED);
                contract.setRemainingAmount(0.0);
            }

            contractRepository.save(contract);
        }

        System.out.println("✅ Mise à jour terminée");
    }
}