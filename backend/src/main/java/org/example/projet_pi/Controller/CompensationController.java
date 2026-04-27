package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.ClaimScoreDTO;
import org.example.projet_pi.Dto.ClientDTO;
import org.example.projet_pi.Dto.CompensationDTO;
import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.Repository.AgentAssuranceRepository;
import org.example.projet_pi.Repository.ClaimRepository;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.Service.AdvancedClaimScoringService;
import org.example.projet_pi.Service.CompensationService;
import org.example.projet_pi.Service.PaymentService;
import org.example.projet_pi.entity.Account;
import org.example.projet_pi.entity.AgentAssurance;
import org.example.projet_pi.entity.Claim;
import org.example.projet_pi.entity.Client;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/compensations")
public class CompensationController {

    private final CompensationService compensationService;
    private final AdvancedClaimScoringService advancedClaimScoringService;
    private final ClaimRepository claimRepository;
    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final PaymentService paymentService; // ✅ Injecter PaymentService pour Stripe
    private final AgentAssuranceRepository agentAssuranceRepository;


    @PostMapping("/addComp")
    public CompensationDTO addCompensation(@RequestBody CompensationDTO dto) {
        return compensationService.addCompensation(dto);
    }

    @PutMapping("/updateComp")
    public CompensationDTO updateCompensation(@RequestBody CompensationDTO dto) {
        return compensationService.updateCompensation(dto);
    }

    @DeleteMapping("/deleteComp/{id}")
    public void deleteCompensation(@PathVariable Long id) {
        compensationService.deleteCompensation(id);
    }

    @GetMapping("/getComp/{id}")
    public CompensationDTO getCompensationById(@PathVariable Long id) {
        return compensationService.getCompensationById(id);
    }

    @GetMapping("/allComp")
    public List<CompensationDTO> getAllCompensations() {
        return compensationService.getAllCompensations();
    }

    // NOUVEAU: Marquer comme payée
    @PostMapping("/{id}/pay")
    public ResponseEntity<CompensationDTO> markAsPaid(@PathVariable Long id) {
        CompensationDTO result = compensationService.markAsPaid(id);
        return ResponseEntity.ok(result);
    }

    // NOUVEAU: Recalculer la compensation
    @PostMapping("/recalculate/{claimId}")
    public ResponseEntity<CompensationDTO> recalculateCompensation(@PathVariable Long claimId) {
        CompensationDTO result = compensationService.recalculateCompensation(claimId);
        return ResponseEntity.ok(result);
    }

    // NOUVEAU: Obtenir les détails avec le message explicatif
    @GetMapping("/{id}/details")
    public ResponseEntity<Map<String, Object>> getCompensationDetails(@PathVariable Long id) {
        CompensationDTO compensation = compensationService.getCompensationById(id);

        Map<String, Object> details = new HashMap<>();
        details.put("compensation", compensation);
        details.put("calculationFormula", Map.of(
                "formula", "min(max(0, approvedAmount - deductible), coverageLimit)",
                "clientOutOfPocket", "approvedAmount - insurancePayment"
        ));

        if (compensation.getMessage() != null) {
            details.put("explanation", compensation.getMessage());
        }

        return ResponseEntity.ok(details);
    }

    // Dans CompensationController.java - AJOUTER

    @GetMapping("/{id}/with-scoring")
    public ResponseEntity<Map<String, Object>> getCompensationWithScoring(@PathVariable Long id) {
        CompensationDTO compensation = compensationService.getCompensationById(id);

        // Récupérer le claim correspondant
        Claim claim = claimRepository.findById(compensation.getClaimId())
                .orElseThrow(() -> new RuntimeException("Claim non trouvé"));

        // Calculer le scoring avancé
        ClaimScoreDTO claimScore = advancedClaimScoringService.calculateAdvancedClaimScore(claim.getClaimId());

        Map<String, Object> response = new HashMap<>();
        response.put("compensation", compensation);
        response.put("claimScore", claimScore);
        response.put("integration", Map.of(
                "status", "FULLY_INTEGRATED",
                "scoreUsedInCalculation", true,
                "adjustmentsApplied", compensation.getAmount() != compensation.getInsurancePayment(),
                "message", "La compensation a été calculée en utilisant le scoring avancé"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * 🔥 NOUVEAU : Le client voit ses propres compensations
     */
    @GetMapping("/my-compensations")
    public ResponseEntity<List<CompensationDTO>> getMyCompensations(
            @AuthenticationPrincipal UserDetails currentUser) {

        Client client = clientRepository.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        List<Claim> clientClaims = claimRepository.findByClientId(client.getId());

        // 🔥 CORRECTION ICI : Filtrer les claims qui ont une compensation
        List<CompensationDTO> compensations = clientClaims.stream()
                .filter(claim -> claim.getCompensation() != null)  // Filtrer d'abord
                .map(claim -> compensationService.getCompensationById(claim.getCompensation().getCompensationId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(compensations);
    }

    /**
     * 🔥 NOUVEAU : Le client paie sa compensation
     */
    @PostMapping("/{id}/pay-by-client")
    public ResponseEntity<Map<String, Object>> payCompensationByClient(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Récupérer le client connecté
            Client client = clientRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));

            // Vérifier que la compensation appartient bien au client
            CompensationDTO compensation = compensationService.getCompensationById(id);
            Claim claim = claimRepository.findById(compensation.getClaimId())
                    .orElseThrow(() -> new RuntimeException("Claim non trouvé"));

            if (!claim.getClient().getId().equals(client.getId())) {
                throw new RuntimeException("Cette compensation ne vous appartient pas !");
            }

            // Vérifier que la compensation n'est pas déjà payée
            if (compensation.getStatus().equals("PAID")) {
                throw new RuntimeException("Cette compensation a déjà été payée !");
            }

            // Effectuer le paiement
            CompensationDTO paidCompensation = compensationService.payCompensation(
                    id, client.getId(), currentUser.getUsername()
            );

            response.put("success", true);
            response.put("message", "Compensation payée avec succès !");
            response.put("compensation", paidCompensation);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 🔥 NOUVEAU : Le client vérifie son solde avant de payer
     */
    @GetMapping("/{id}/check-balance")
    public ResponseEntity<Map<String, Object>> checkBalanceBeforePayment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Récupérer le client connecté
            Client client = clientRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));

            // Vérifier que la compensation appartient au client
            CompensationDTO compensation = compensationService.getCompensationById(id);
            Claim claim = claimRepository.findById(compensation.getClaimId())
                    .orElseThrow(() -> new RuntimeException("Claim non trouvé"));

            if (!claim.getClient().getId().equals(client.getId())) {
                throw new RuntimeException("Cette compensation ne vous appartient pas !");
            }

            // Récupérer le compte du client
            Account account = accountRepository.findByClientId(client.getId()).stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Aucun compte trouvé pour ce client"));

            double amountToPay = compensation.getAmount();
            double currentBalance = account.getBalance();

            response.put("success", true);
            response.put("compensationId", id);
            response.put("amountToPay", amountToPay);
            response.put("currentBalance", currentBalance);
            response.put("sufficientBalance", currentBalance >= amountToPay);
            response.put("difference", currentBalance - amountToPay);

            if (currentBalance < amountToPay) {
                response.put("warning", String.format(
                        "Solde insuffisant. Manque: %.2f DT", amountToPay - currentBalance
                ));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 🔥 NOUVEAU : Le client voit le détail d'une compensation avec scoring
     */
    @GetMapping("/{id}/my-details")
    public ResponseEntity<Map<String, Object>> getMyCompensationDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();

        try {
            Client client = clientRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));

            CompensationDTO compensation = compensationService.getCompensationById(id);
            Claim claim = claimRepository.findById(compensation.getClaimId())
                    .orElseThrow(() -> new RuntimeException("Claim non trouvé"));

            if (!claim.getClient().getId().equals(client.getId())) {
                throw new RuntimeException("Cette compensation ne vous appartient pas !");
            }

            // Calculer le scoring avancé
            ClaimScoreDTO claimScore = advancedClaimScoringService.calculateAdvancedClaimScore(claim.getClaimId());

            response.put("compensation", compensation);
            response.put("claim", claim);
            response.put("claimScore", claimScore);
            response.put("status", "OK");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    /**
     * 🔥 NOUVEAU : Paiement par CARTE (Stripe)
     */
    @PostMapping("/{id}/pay-by-card")
    public ResponseEntity<Map<String, Object>> payCompensationByCard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();

        try {
            Client client = clientRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));

            CompensationDTO compensation = compensationService.getCompensationById(id);
            Claim claim = claimRepository.findById(compensation.getClaimId())
                    .orElseThrow(() -> new RuntimeException("Claim non trouvé"));

            if (!claim.getClient().getId().equals(client.getId())) {
                throw new RuntimeException("Cette compensation ne vous appartient pas !");
            }

            if (compensation.getStatus().equals("PAID")) {
                throw new RuntimeException("Cette compensation a déjà été payée !");
            }

            // Le montant à payer est le reste à charge (clientOutOfPocket)
            double amountToPay = compensation.getClientOutOfPocket();

            // Créer un PaymentIntent Stripe
            com.stripe.model.PaymentIntent paymentIntent = paymentService.createCompensationPaymentIntent(
                    compensation.getCompensationId(), amountToPay, client.getEmail()
            );

            response.put("success", true);
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentIntentId", paymentIntent.getId());
            response.put("amount", amountToPay);
            response.put("compensationId", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 🔥 NOUVEAU : Paiement en CASH
     */
    @PostMapping("/{id}/pay-by-cash")
    public ResponseEntity<Map<String, Object>> payCompensationByCash(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();

        try {
            Client client = clientRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));

            CompensationDTO compensation = compensationService.getCompensationById(id);
            Claim claim = claimRepository.findById(compensation.getClaimId())
                    .orElseThrow(() -> new RuntimeException("Claim non trouvé"));

            if (!claim.getClient().getId().equals(client.getId())) {
                throw new RuntimeException("Cette compensation ne vous appartient pas !");
            }

            if (compensation.getStatus().equals("PAID")) {
                throw new RuntimeException("Cette compensation a déjà été payée !");
            }

            // Marquer la compensation comme payée
            CompensationDTO paidCompensation = compensationService.markAsPaid(id);

            response.put("success", true);
            response.put("message", "Paiement en cash enregistré avec succès !");
            response.put("compensation", paidCompensation);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 🔥 NOUVEAU : Confirmer le paiement Stripe
     */
    @PostMapping("/confirm-payment/{paymentIntentId}")
    public ResponseEntity<Map<String, Object>> confirmCompensationPayment(
            @PathVariable String paymentIntentId,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();

        try {
            Client client = clientRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé"));

            // Récupérer les métadonnées du PaymentIntent
            com.stripe.model.PaymentIntent paymentIntent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
            Map<String, String> metadata = paymentIntent.getMetadata();

            Long compensationId = Long.parseLong(metadata.get("compensationId"));

            CompensationDTO compensation = compensationService.getCompensationById(compensationId);
            Claim claim = claimRepository.findById(compensation.getClaimId())
                    .orElseThrow(() -> new RuntimeException("Claim non trouvé"));

            if (!claim.getClient().getId().equals(client.getId())) {
                throw new RuntimeException("Cette compensation ne vous appartient pas !");
            }

            if (compensation.getStatus().equals("PAID")) {
                throw new RuntimeException("Cette compensation a déjà été payée !");
            }

            // Marquer comme payée
            CompensationDTO paidCompensation = compensationService.markAsPaid(compensationId);

            response.put("success", true);
            response.put("message", "Paiement par carte effectué avec succès !");
            response.put("compensation", paidCompensation);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Dans CompensationController.java, ajoutez ces méthodes :

    /**
     * 🔥 NOUVEAU : L'agent d'assurance voit les compensations de ses clients
     */
    /**
     * 🔥 NOUVEAU : L'agent d'assurance voit les compensations de ses clients
     */
    @GetMapping("/agent/compensations")
    public ResponseEntity<List<CompensationDTO>> getAgentCompensations(
            @AuthenticationPrincipal UserDetails currentUser) {

        // Récupérer l'agent connecté
        AgentAssurance agent = agentAssuranceRepository.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        // Récupérer tous les clients de cet agent
        List<Client> clients = clientRepository.findByAgentAssuranceId(agent.getId());

        // Récupérer toutes les compensations des clients
        List<CompensationDTO> compensations = new ArrayList<>();

        for (Client client : clients) {
            List<Claim> clientClaims = claimRepository.findByClientId(client.getId());
            for (Claim claim : clientClaims) {
                if (claim.getCompensation() != null) {
                    CompensationDTO compensationDTO = compensationService.getCompensationById(
                            claim.getCompensation().getCompensationId()
                    );

                    // 🔥 Ajouter explicitement les informations du client
                    ClientDTO clientDTO = new ClientDTO();
                    clientDTO.setId(client.getId());
                    clientDTO.setFirstName(client.getFirstName());
                    clientDTO.setLastName(client.getLastName());
                    clientDTO.setEmail(client.getEmail());
                    clientDTO.setTelephone(client.getTelephone());
                    compensationDTO.setClient(clientDTO);

                    compensations.add(compensationDTO);
                }
            }
        }

        return ResponseEntity.ok(compensations);
    }


    /**
     * 🔥 NOUVEAU : L'agent d'assurance voit la liste de ses clients
     */
    @GetMapping("/agent/clients")
    public ResponseEntity<List<ClientDTO>> getAgentClients(
            @AuthenticationPrincipal UserDetails currentUser) {

        AgentAssurance agent = agentAssuranceRepository.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

        List<Client> clients = clientRepository.findByAgentAssuranceId(agent.getId());

        List<ClientDTO> clientDTOs = clients.stream()
                .map(client -> {
                    ClientDTO dto = new ClientDTO();
                    dto.setId(client.getId());
                    dto.setFirstName(client.getFirstName());
                    dto.setLastName(client.getLastName());
                    dto.setEmail(client.getEmail());
                    dto.setTelephone(client.getTelephone());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(clientDTOs);
    }

}