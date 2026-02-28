package org.example.projet_pi.Controller;

import org.example.projet_pi.Service.IRepaymentService;
import org.example.projet_pi.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/Repayment")
public class RepaymentController {

    private final IRepaymentService repaymentService;

    public RepaymentController(IRepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    // ===============================
    // PAYER UN CRÉDIT - CLIENT SEULEMENT
    // ===============================
    @PostMapping("/pay-credit/{creditId}")
    public ResponseEntity<?> payCredit(
            @PathVariable Long creditId,
            @RequestBody Repayment repayment,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "CLIENT")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé",
                                "message", "Seul un client peut effectuer un paiement"));
            }

            // Récupérer le client complet
            Client client = getClientFromUserDetails(currentUser);
            repayment.setClient(client);

            Repayment result = repaymentService.payCredit(creditId, repayment);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur lors du paiement", "message", e.getMessage()));
        }
    }

    // ===============================
    // MONTANT RESTANT - TOUS LES RÔLES
    // ===============================
    @GetMapping("/remaining/{creditId}")
    public ResponseEntity<?> getRemainingAmount(
            @PathVariable Long creditId,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Non authentifié"));
            }

            BigDecimal remaining = repaymentService.getRemainingAmount(creditId);
            return ResponseEntity.ok(Map.of(
                    "creditId", creditId,
                    "remainingAmount", remaining,
                    "currency", "TND"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur", "message", e.getMessage()));
        }
    }

    // ===============================
    // MES PAIEMENTS - CLIENT
    // ===============================
    @GetMapping("/myPayments")
    public ResponseEntity<?> getMyPayments(Authentication authentication) {
        try {
            String email = authentication.getName();
            List<Repayment> myPayments = repaymentService.getRepaymentsByClientEmail(email);
            return ResponseEntity.ok(myPayments);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur", "message", e.getMessage()));
        }
    }

    // ===============================
    // HISTORIQUE DES PAIEMENTS - AGENT FINANCE ET ADMIN
    // ===============================
    @GetMapping("/history/{creditId}")
    public ResponseEntity<?> getRepaymentHistory(
            @PathVariable Long creditId,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "AGENT_FINANCE") && !hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé",
                                "message", "Réservé aux agents finance et admins"));
            }

            List<Repayment> history = repaymentService.getRepaymentsByCreditId(creditId);
            return ResponseEntity.ok(Map.of(
                    "creditId", creditId,
                    "totalPayments", history.size(),
                    "payments", history
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur", "message", e.getMessage()));
        }
    }

    // ===============================
    // TOUS LES PAIEMENTS - AGENT FINANCE ET ADMIN
    // ===============================
    @GetMapping("/allRepayment")
    public ResponseEntity<?> getAllRepayments(@AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "AGENT_FINANCE") && !hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé",
                                "message", "Réservé aux agents finance et admins"));
            }

            List<Repayment> repayments = repaymentService.getAllRepayments();
            return ResponseEntity.ok(repayments);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur", "message", e.getMessage()));
        }
    }

    // ===============================
    // GET REPAYMENT BY ID - ADMIN SEULEMENT
    // ===============================
    @GetMapping("/getRepayment/{id}")
    public ResponseEntity<?> getRepaymentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé", "message", "Admin seulement"));
            }

            Repayment repayment = repaymentService.getRepaymentById(id);
            if (repayment == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Paiement non trouvé", "id", id));
            }
            return ResponseEntity.ok(repayment);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur", "message", e.getMessage()));
        }
    }

    // ===============================
    // ADD REPAYMENT - ADMIN SEULEMENT
    // ===============================
    @PostMapping("/addRepayment")
    public ResponseEntity<?> addRepayment(
            @RequestBody Repayment repayment,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé", "message", "Admin seulement"));
            }

            Repayment saved = repaymentService.addRepayment(repayment);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur lors de l'ajout", "message", e.getMessage()));
        }
    }

    // ===============================
    // UPDATE REPAYMENT - ADMIN SEULEMENT
    // ===============================
    @PutMapping("/updateRepayment")
    public ResponseEntity<?> updateRepayment(
            @RequestBody Repayment repayment,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé", "message", "Admin seulement"));
            }

            Repayment updated = repaymentService.updateRepayment(repayment);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur lors de la modification", "message", e.getMessage()));
        }
    }

    // ===============================
    // DELETE REPAYMENT - ADMIN SEULEMENT
    // ===============================
    @DeleteMapping("/deleteRepayment/{id}")
    public ResponseEntity<?> deleteRepayment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé", "message", "Admin seulement"));
            }

            repaymentService.deleteRepayment(id);
            return ResponseEntity.ok(Map.of("message", "Paiement supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur lors de la suppression", "message", e.getMessage()));
        }
    }

    // ===============================
    // UTILITAIRES
    // ===============================
    private boolean hasRole(UserDetails userDetails, String role) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private Client getClientFromUserDetails(UserDetails userDetails) {
        // Implémentez la logique pour récupérer l'entité Client complète
        // à partir de l'email ou username
        return null; // À implémenter selon votre code
    }
}