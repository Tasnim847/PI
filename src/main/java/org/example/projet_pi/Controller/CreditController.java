package org.example.projet_pi.Controller;

import org.example.projet_pi.Dto.CreditHistoryDTO;
import org.example.projet_pi.Service.AdminService;
import org.example.projet_pi.Service.CreditService;
import org.example.projet_pi.Service.EmailCredit.CreditNotificationScheduler;
import org.example.projet_pi.Service.IClientService;
import org.example.projet_pi.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.example.projet_pi.Dto.CreditHistoryWithAverageDTO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/Credit")
public class CreditController {

    private final AdminService adminService;
    private final CreditService creditService;
    @Autowired
    private IClientService clientService;

    public CreditController(CreditService creditService , AdminService adminService) {
        this.creditService = creditService;
        this.adminService = adminService;
    }

    // ===============================
    // CREATE CREDIT - ADMIN SEULEMENT
    // ===============================
    @PostMapping("/addCredit")
    public ResponseEntity<?> addCredit(
            @RequestBody Credit credit,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            // Vérification que l'utilisateur est admin
            if (!hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé", "message", "Seul l'admin peut ajouter un crédit"));
            }

            // ✅ Récupérer l'admin connecté
            Admin admin = getAdminFromUserDetails(currentUser);

            // ✅ Appeler le service avec l'admin
            Credit savedCredit = creditService.addCredit(credit, admin);

            return ResponseEntity.ok(savedCredit);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur lors de l'ajout", "message", e.getMessage()));
        }
    }

    private Admin getAdminFromUserDetails(UserDetails userDetails) {
        // Récupérer l'email de l'utilisateur connecté
        String email = userDetails.getUsername();

        // Chercher l'admin dans la base de données par email
        return adminService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé avec l'email: " + email));
    }

    // ===============================
    // APPROVE - AGENT FINANCE OU ADMIN
    // ===============================
    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveCredit(
            @PathVariable Long id,
            @RequestParam double interestRate,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            // Vérification des rôles
            if (!hasRole(currentUser, "AGENT_FINANCE") && !hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé",
                                "message", "Seul un agent finance ou admin peut approuver un crédit"));
            }

            Credit approvedCredit = creditService.approveCredit(id, interestRate);

            // Récupérer l'utilisateur complet pour l'association
            User user = getUserFromUserDetails(currentUser);
            if (user instanceof AgentFinance) {
                approvedCredit.setAgentFinance((AgentFinance) user);
            } else if (user instanceof Admin) {
                approvedCredit.setAdmin((Admin) user);
            }

            creditService.updateCredit(approvedCredit);
            return ResponseEntity.ok(approvedCredit);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur lors de l'approbation", "message", e.getMessage()));
        }
    }

    // ===============================
    // REJECT - AGENT FINANCE OU ADMIN
    // ===============================
    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectCredit(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "AGENT_FINANCE") && !hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé",
                                "message", "Seul un agent finance ou admin peut rejeter un crédit"));
            }

            Credit rejectedCredit = creditService.rejectCredit(id);
            return ResponseEntity.ok(rejectedCredit);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur lors du rejet", "message", e.getMessage()));
        }
    }

    // ===============================
    // UPDATE - ADMIN SEULEMENT
    // ===============================
    @PutMapping("/updateCredit")
    public ResponseEntity<?> updateCredit(
            @RequestBody Credit credit,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé", "message", "Admin seulement"));
            }

            Credit updatedCredit = creditService.updateCredit(credit);
            return ResponseEntity.ok(updatedCredit);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur lors de la modification", "message", e.getMessage()));
        }
    }

    // ===============================
    // DELETE - ADMIN SEULEMENT
    // ===============================
    @DeleteMapping("/deleteCredit/{id}")
    public ResponseEntity<?> deleteCredit(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé", "message", "Admin seulement"));
            }

            creditService.deleteCredit(id);
            return ResponseEntity.ok(Map.of("message", "Crédit supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur lors de la suppression", "message", e.getMessage()));
        }
    }

    // ===============================
    // GET ALL CREDITS - AGENT FINANCE ET ADMIN
    // ===============================
    @GetMapping("/allCredit")
    public ResponseEntity<?> getAllCredits(@AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "AGENT_FINANCE") && !hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé",
                                "message", "Réservé aux agents finance et admins"));
            }

            List<Credit> credits = creditService.getAllCredits();
            return ResponseEntity.ok(credits);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur", "message", e.getMessage()));
        }
    }

    // ===============================
    // GET CREDIT BY ID - ADMIN SEULEMENT
    // ===============================
    @GetMapping("/getCredit/{id}")
    public ResponseEntity<?> getCreditById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé", "message", "Admin seulement"));
            }

            Credit credit = creditService.getCreditById(id);
            if (credit == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Crédit non trouvé", "id", id));
            }
            return ResponseEntity.ok(credit);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur", "message", e.getMessage()));
        }
    }

    // ===============================
    // CLOSED CREDITS WITH AVERAGE - AGENT FINANCE ET ADMIN
    // ===============================
    @GetMapping("/closedCreditsWithAverage/{clientId}")
    public ResponseEntity<?> getClosedCreditsWithAverage(
            @PathVariable Long clientId,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "AGENT_FINANCE") && !hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé",
                                "message", "Réservé aux agents finance et admins"));
            }

            Client client = clientService.getClientById(clientId);
            if (client == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Client non trouvé", "clientId", clientId));
            }

            CreditHistoryWithAverageDTO result = creditService.getClosedCreditsWithAverage(client);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur", "message", e.getMessage()));
        }
    }

    // ===============================
    // MES CREDITS - POUR LE CLIENT CONNECTÉ
    // ===============================
    @GetMapping("/myCredits")
    public ResponseEntity<?> getMyCredits(Authentication authentication) {
        try {
            String email = authentication.getName();
            List<Credit> myCredits = creditService.getCreditsByClientEmail(email);
            return ResponseEntity.ok(myCredits);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur", "message", e.getMessage()));
        }
    }

    // ===============================
    // UTILITAIRES
    // ===============================
    private boolean hasRole(UserDetails userDetails, String role) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private User getUserFromUserDetails(UserDetails userDetails) {
        // Implémentez la logique pour récupérer l'entité User complète
        // à partir de l'email ou username
        return null; // À implémenter selon votre code
    }

    @Autowired
    private CreditNotificationScheduler notificationScheduler;

    @PostMapping("/test-notification/{creditId}")
    public ResponseEntity<?> testNotification(@PathVariable Long creditId) {
        try {
            notificationScheduler.testReminderForCredit(creditId);
            return ResponseEntity.ok(Map.of(
                    "message", "Email de test envoyé avec succès",
                    "creditId", creditId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Erreur: " + e.getMessage()
            ));
        }
    }
}