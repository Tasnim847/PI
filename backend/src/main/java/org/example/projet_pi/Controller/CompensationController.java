package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.ClaimScoreDTO;
import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.Repository.ClaimRepository;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.Service.AdvancedClaimScoringService;
import org.example.projet_pi.Service.CompensationService;
import org.example.projet_pi.Dto.CompensationDTO;
import org.example.projet_pi.entity.Account;
import org.example.projet_pi.entity.Claim;
import org.example.projet_pi.entity.Client;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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

}