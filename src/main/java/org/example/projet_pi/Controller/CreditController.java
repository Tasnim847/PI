package org.example.projet_pi.Controller;

import org.example.projet_pi.Dto.CreditHistoryDTO;
import org.example.projet_pi.Dto.CreditRequestDTO;
import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.Repository.CreditRepository;
import org.example.projet_pi.Service.AdminService;
import org.example.projet_pi.Service.CreditService;
import org.example.projet_pi.Service.EmailCredit.CreditEmailService;
import org.example.projet_pi.Service.EmailCredit.CreditNotificationScheduler;
import org.example.projet_pi.Service.IClientService;
import org.example.projet_pi.entity.*;
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
    private final IClientService clientService;
    private final CreditNotificationScheduler notificationScheduler;
    private final AccountRepository accountRepository;
    private final CreditRepository creditRepository;
    private final CreditEmailService creditEmailService;

    public CreditController(CreditService creditService,
                            AdminService adminService,
                            IClientService clientService,
                            CreditNotificationScheduler notificationScheduler,
                            AccountRepository accountRepository,
                            CreditRepository creditRepository,
                            CreditEmailService creditEmailService) {
        this.creditService = creditService;
        this.adminService = adminService;
        this.clientService = clientService;
        this.notificationScheduler = notificationScheduler;
        this.accountRepository = accountRepository;
        this.creditRepository = creditRepository;
        this.creditEmailService = creditEmailService;
    }

    // ===============================
    // CREATE CREDIT - ADMIN SEULEMENT
    // ===============================
    @PostMapping("/addCredit")
    public ResponseEntity<?> addCredit(
            @RequestBody Credit credit,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (!hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé", "message", "Seul l'admin peut ajouter un crédit"));
            }

            Admin admin = getAdminFromUserDetails(currentUser);
            Credit savedCredit = creditService.addCredit(credit, admin);

            return ResponseEntity.ok(savedCredit);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur lors de l'ajout", "message", e.getMessage()));
        }
    }

    // ===============================
    // DEMANDE DE CRÉDIT - CLIENT
    // ===============================
    @PostMapping("/requestCredit")
    public ResponseEntity<?> requestCredit(
            @RequestBody CreditRequestDTO request,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            // Vérifier que c'est un client
            if (!hasRole(currentUser, "CLIENT")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé - Client seulement"));
            }

            // ✅ Récupérer le client connecté en filtrant la liste des clients
            String email = currentUser.getUsername();
            Client client = null;

            List<Client> allClients = clientService.getAllClients();
            for (Client c : allClients) {
                if (c.getEmail() != null && c.getEmail().equals(email)) {
                    client = c;
                    break;
                }
            }

            if (client == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Client non trouvé avec l'email: " + email));
            }

            // Vérifier que le client a un compte
            List<Account> accounts = accountRepository.findByClientId(client.getId());
            if (accounts.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Vous devez avoir un compte bancaire pour demander un crédit"));
            }

            // Vérifier l'historique de retards
            List<Credit> closedCredits = creditRepository.findByClientAndStatus(client, CreditStatus.CLOSED);
            double averageLatePercentage = calculateAverageLatePercentage(closedCredits);

            // Si historique > 40%, rejet automatique
            if (averageLatePercentage > 40) {
                String clientName = client.getFirstName() + " " + client.getLastName();
                creditEmailService.sendAutoRejectionNotification(
                        client.getEmail(),
                        clientName,
                        request.getAmount(),
                        averageLatePercentage
                );

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Demande rejetée",
                                "reason", "Historique de retards trop élevé (" + averageLatePercentage + "%)",
                                "required", "Moins de 40%"
                        ));
            }

            // Créer le crédit en PENDING
            Credit credit = new Credit();
            credit.setAmount(request.getAmount());
            credit.setDurationInMonths(request.getDurationInMonths());
            credit.setDueDate(request.getDueDate());
            credit.setClient(client);
            credit.setStatus(CreditStatus.PENDING);

            // Sauvegarder le crédit
            Credit savedCredit = creditService.addCredit(credit, null);

            // Envoyer notification à l'admin
            sendNotificationToAdmins(savedCredit, client);

            return ResponseEntity.ok(Map.of(
                    "message", "Votre demande de crédit a été envoyée avec succès",
                    "creditId", savedCredit.getCreditId(),
                    "status", "PENDING",
                    "amount", savedCredit.getAmount(),
                    "duration", savedCredit.getDurationInMonths(),
                    "dueDate", savedCredit.getDueDate()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private Admin getAdminFromUserDetails(UserDetails userDetails) {
        String email = userDetails.getUsername();
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
            if (!hasRole(currentUser, "AGENT_FINANCE") && !hasRole(currentUser, "ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès refusé",
                                "message", "Seul un agent finance ou admin peut approuver un crédit"));
            }

            Credit approvedCredit = creditService.approveCredit(id, interestRate);

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
    // TEST NOTIFICATION ENDPOINT
    // ===============================
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

    // ===============================
    // UTILITAIRES
    // ===============================
    private boolean hasRole(UserDetails userDetails, String role) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private User getUserFromUserDetails(UserDetails userDetails) {
        return null;
    }

    // ===============================
    // MÉTHODES PRIVÉES
    // ===============================

    private double calculateAverageLatePercentage(List<Credit> closedCredits) {
        if (closedCredits.isEmpty()) return 0;

        double totalLatePercentage = 0;
        for (Credit credit : closedCredits) {
            long totalRepayments = credit.getRepayments().size();
            long lateRepayments = credit.getRepayments().stream()
                    .filter(r -> r.getStatus() == RepaymentStatus.LATE)
                    .count();

            double latePercentage = totalRepayments > 0 ?
                    ((double) lateRepayments / totalRepayments) * 100 : 0;
            totalLatePercentage += latePercentage;
        }
        return totalLatePercentage / closedCredits.size();
    }

    private void sendNotificationToAdmins(Credit credit, Client client) {
        try {
            List<Admin> admins = adminService.getAllAdmins();

            String clientName = client.getFirstName() + " " + client.getLastName();

            for (Admin admin : admins) {
                if (admin.getEmail() != null && !admin.getEmail().isEmpty()) {
                    creditEmailService.sendCreditRequestNotification(
                            admin.getEmail(),
                            admin.getFirstName() + " " + admin.getLastName(),
                            clientName,
                            credit.getAmount(),
                            credit.getDurationInMonths(),
                            credit.getCreditId()
                    );
                }
            }

            System.out.println("📧 Notification envoyée à " + admins.size() + " admins");

        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi des notifications: " + e.getMessage());
        }
    }
}