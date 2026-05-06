package org.example.projet_pi.Controller;

import org.example.projet_pi.Dto.ComplaintSearchDTO;
import org.example.projet_pi.Repository.ComplaintRepository;
import org.example.projet_pi.Repository.UserRepository;
import org.example.projet_pi.Service.SmsService3;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Complaint;
import org.example.projet_pi.entity.Role;
import org.example.projet_pi.entity.User;
import org.example.projet_pi.security.CustomUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/complaints")
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final SmsService3 smsService3;

    public ComplaintController(ComplaintRepository complaintRepository, UserRepository userRepository,
                               SmsService3 smsService3) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.smsService3 = smsService3;
    }

    // ========== CRUD ==========

    /**
     * AJOUTER une réclamation
     * POST /complaints/addComplaint
     */
    @PostMapping("/addComplaint")
    @Transactional
    public ResponseEntity<?> addComplaint(@RequestBody Complaint complaint) {
        try {
            System.out.println("=== ADD COMPLAINT ===");

            // 1. Valider et charger le client
            validateAndLoadUsers(complaint);

            // 2. Récupérer l'utilisateur connecté
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = null;

            if (auth.getPrincipal() instanceof CustomUserPrincipal) {
                String email = auth.getName();
                currentUser = userRepository.findByEmail(email).orElse(null);
            }

            // 3. Affecter l'agent correspondant
            Client client = (Client) complaint.getClient();

            // Si l'utilisateur connecté est un agent d'assurance
            if (currentUser != null && currentUser.getRole() == Role.AGENT_ASSURANCE) {
                complaint.setAgentAssurance(currentUser);
                System.out.println("✅ Affecté à l'agent d'assurance: " + currentUser.getEmail());
            }
            // Si l'utilisateur connecté est un agent financier
            else if (currentUser != null && currentUser.getRole() == Role.AGENT_FINANCE) {
                complaint.setAgentFinance(currentUser);
                System.out.println("✅ Affecté à l'agent financier: " + currentUser.getEmail());
            }
            // Sinon (client qui crée), utiliser les agents déjà affectés au client
            else {
                if (client.getAgentAssurance() != null) {
                    complaint.setAgentAssurance(client.getAgentAssurance());
                    System.out.println("✅ Affecté à l'agent assurance du client");
                }
                if (client.getAgentFinance() != null) {
                    complaint.setAgentFinance(client.getAgentFinance());
                    System.out.println("✅ Affecté à l'agent finance du client");
                }
            }

            // 4. Définir les dates et statut
            if (complaint.getClaimDate() == null) {
                complaint.setClaimDate(new Date());
            }
            if (complaint.getStatus() == null || complaint.getStatus().isEmpty()) {
                complaint.setStatus("PENDING");
            }

            // 5. Sauvegarder
            Complaint saved = complaintRepository.save(complaint);

            // 6. Envoyer SMS
            if (saved.getPhone() != null && !saved.getPhone().isEmpty()) {
                try {
                    smsService3.sendSms(saved.getPhone(),
                            "Votre réclamation a été enregistrée avec succès.");
                } catch (Exception e) {
                    System.out.println("Erreur SMS: " + e.getMessage());
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Réclamation ajoutée avec succès", "complaint", saved));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * MODIFIER une réclamation
     * PUT /complaints/updateComplaint/{id}
     */
    @PutMapping("/updateComplaint/{id}")
    @Transactional
    public ResponseEntity<?> updateComplaint(@PathVariable Long id, @RequestBody Complaint complaintDetails) {
        try {
            Complaint existingComplaint = complaintRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Réclamation non trouvée avec l'id: " + id));

            if (complaintDetails.getStatus() != null) {
                existingComplaint.setStatus(complaintDetails.getStatus());
            }

            if (complaintDetails.getMessage() != null) {
                existingComplaint.setMessage(complaintDetails.getMessage());
            }

            if (complaintDetails.getPhone() != null) {
                existingComplaint.setPhone(complaintDetails.getPhone());
            }

            if (complaintDetails.getClaimDate() != null) {
                existingComplaint.setClaimDate(complaintDetails.getClaimDate());
            }

            if (complaintDetails.getResolutionDate() != null) {
                existingComplaint.setResolutionDate(complaintDetails.getResolutionDate());
            }

            Complaint updatedComplaint = complaintRepository.save(existingComplaint);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Réclamation modifiée avec succès");
            response.put("complaint", updatedComplaint);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Erreur lors de la modification",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * SUPPRIMER une réclamation
     * DELETE /complaints/deleteComplaint/{id}
     */
    @DeleteMapping("/deleteComplaint/{id}")
    @Transactional
    public ResponseEntity<?> deleteComplaint(@PathVariable Long id) {
        try {
            Complaint complaint = complaintRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Réclamation non trouvée avec l'id: " + id));

            complaintRepository.delete(complaint);

            return ResponseEntity.ok(Map.of(
                    "message", "Réclamation supprimée avec succès",
                    "id", id
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Erreur lors de la suppression",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * RÉCUPÉRER une réclamation par ID
     * GET /complaints/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getComplaintById(@PathVariable Long id) {
        try {
            Complaint complaint = complaintRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Réclamation non trouvée"));
            return ResponseEntity.ok(complaint);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * RÉCUPÉRER toutes les réclamations
     * GET /complaints/all
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllComplaints() {
        try {
            List<Complaint> complaints = complaintRepository.findAll();
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Liste des réclamations");
            response.put("count", complaints.size());
            response.put("complaints", complaints);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== RECHERCHE ==========

    @PostMapping("/search")
    public ResponseEntity<?> searchComplaints(@RequestBody ComplaintSearchDTO dto) {
        try {
            List<Complaint> results = complaintRepository.searchComplaints(
                    dto.getStatus(),
                    dto.getKeyword(),
                    dto.getClientId(),
                    dto.getAgentAssuranceId(),
                    dto.getAgentFinanceId(),
                    dto.getDateDebut(),
                    dto.getDateFin()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Recherche effectuée");
            response.put("count", results.size());
            response.put("results", results);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== KPI ==========

    @GetMapping("/kpi/average-processing-time")
    public ResponseEntity<?> calculateAverageProcessingTime() {
        try {
            List<Complaint> closed = complaintRepository.findByStatus("CLOSED");
            if (closed.isEmpty()) return ResponseEntity.ok(0.0);

            long totalDays = 0;
            int count = 0;

            for (Complaint c : closed) {
                if (c.getClaimDate() != null && c.getResolutionDate() != null) {
                    long days = ChronoUnit.DAYS.between(
                            c.getClaimDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                            c.getResolutionDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    );
                    totalDays += days;
                    count++;
                }
            }

            double average = count == 0 ? 0 : (double) totalDays / count;
            return ResponseEntity.ok(Map.of("averageProcessingTime", average, "unit", "days"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/kpi/resolution-rate")
    public ResponseEntity<?> resolutionRate() {
        try {
            long total = complaintRepository.count();
            if (total == 0) return ResponseEntity.ok(0.0);
            long resolved = complaintRepository.countByStatus("APPROVED");
            double rate = (resolved * 100.0) / total;
            return ResponseEntity.ok(Map.of("resolutionRate", rate, "unit", "percentage"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/kpi/rejection-rate")
    public ResponseEntity<?> rejectionRate() {
        try {
            long total = complaintRepository.count();
            if (total == 0) return ResponseEntity.ok(0.0);
            long rejected = complaintRepository.countByStatus("REJECTED");
            double rate = (rejected * 100.0) / total;
            return ResponseEntity.ok(Map.of("rejectionRate", rate, "unit", "percentage"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/kpi/top-agent")
    public ResponseEntity<?> findTopAgent() {
        try {
            List<Complaint> complaints = complaintRepository.findAll();
            Map<String, Long> agentCount = new HashMap<>();

            for (Complaint c : complaints) {
                if (c.getAgentAssurance() != null) {
                    User agent = c.getAgentAssurance();
                    String fullName = agent.getFirstName() + " " + agent.getLastName() + " (Assurance)";
                    agentCount.put(fullName, agentCount.getOrDefault(fullName, 0L) + 1);
                }
                if (c.getAgentFinance() != null) {
                    User agent = c.getAgentFinance();
                    String fullName = agent.getFirstName() + " " + agent.getLastName() + " (Finance)";
                    agentCount.put(fullName, agentCount.getOrDefault(fullName, 0L) + 1);
                }
            }

            String topAgent = agentCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Aucun agent");

            return ResponseEntity.ok(Map.of("topAgent", topAgent));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/kpi/dashboard")
    public ResponseEntity<?> getDashboardKpi() {
        try {
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("averageProcessingTime", calculateAverageProcessingTime().getBody());
            dashboard.put("resolutionRate", resolutionRate().getBody());
            dashboard.put("rejectionRate", rejectionRate().getBody());
            dashboard.put("topAgent", findTopAgent().getBody());

            long total = complaintRepository.count();
            long pending = complaintRepository.countByStatus("PENDING");
            long inProgress = complaintRepository.countByStatus("IN_PROGRESS");
            long approved = complaintRepository.countByStatus("APPROVED");
            long rejected = complaintRepository.countByStatus("REJECTED");
            long closed = complaintRepository.countByStatus("CLOSED");

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("pending", pending);
            stats.put("inProgress", inProgress);
            stats.put("approved", approved);
            stats.put("rejected", rejected);
            stats.put("closed", closed);

            dashboard.put("statistics", stats);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== MÉTHODE DE VALIDATION CORRIGÉE ==========

    private void validateAndLoadUsers(Complaint complaint) {
        // Client (obligatoire) - Cherche d'abord par EMAIL
        if (complaint.getClient() != null) {
            User foundClient = null;

            // 1. Chercher par EMAIL (prioritaire)
            if (complaint.getClient().getEmail() != null && !complaint.getClient().getEmail().isEmpty()) {
                Optional<User> existingClient = userRepository.findByEmail(complaint.getClient().getEmail());
                if (existingClient.isPresent()) {
                    foundClient = existingClient.get();
                    System.out.println("✅ Client trouvé par email: " + foundClient.getEmail() + " (ID: " + foundClient.getId() + ")");
                }
            }

            // 2. Si pas trouvé, chercher par ID
            if (foundClient == null && complaint.getClient().getId() != null) {
                Optional<User> existingClient = userRepository.findById(complaint.getClient().getId());
                if (existingClient.isPresent()) {
                    foundClient = existingClient.get();
                    System.out.println("✅ Client trouvé par ID: " + foundClient.getId());
                }
            }

            // 3. Si trouvé, utiliser l'utilisateur existant
            if (foundClient != null) {
                complaint.setClient(foundClient);
            } else {
                // 4. Sinon, créer un nouveau client
                User newClient = complaint.getClient();
                newClient.setId(null);
                if (newClient.getEmail() == null || newClient.getEmail().isEmpty()) {
                    newClient.setEmail("client_" + System.currentTimeMillis() + "@test.com");
                }
                newClient.setRole(Role.CLIENT);
                if (newClient.getPassword() == null || newClient.getPassword().isEmpty()) {
                    newClient.setPassword("$2a$10$dummyhash");
                }
                if (newClient.getFirstName() == null || newClient.getFirstName().isEmpty()) {
                    newClient.setFirstName("Client");
                }
                if (newClient.getLastName() == null || newClient.getLastName().isEmpty()) {
                    newClient.setLastName("Test");
                }
                complaint.setClient(userRepository.save(newClient));
                System.out.println("🆕 Nouveau client créé: " + newClient.getEmail());
            }
        } else {
            throw new RuntimeException("Le client est obligatoire");
        }
    }
    // Ajoutez cette méthode dans votre ComplaintController.java (à la fin de la classe, avant la dernière accolade)

    @GetMapping("/all-simple")
    public ResponseEntity<?> getAllComplaintsSimple() {
        try {
            List<Complaint> complaints = complaintRepository.findAll();

            // Créer une liste simplifiée sans les références circulaires
            List<Map<String, Object>> simpleComplaints = new ArrayList<>();

            for (Complaint c : complaints) {
                Map<String, Object> simpleComplaint = new HashMap<>();
                simpleComplaint.put("id", c.getId());
                simpleComplaint.put("status", c.getStatus());
                simpleComplaint.put("message", c.getMessage());
                simpleComplaint.put("claimDate", c.getClaimDate());
                simpleComplaint.put("resolutionDate", c.getResolutionDate());
                simpleComplaint.put("phone", c.getPhone());

                // Ajouter les infos client sans les réclamations imbriquées
                if (c.getClient() != null) {
                    Map<String, Object> simpleClient = new HashMap<>();
                    simpleClient.put("id", c.getClient().getId());
                    simpleClient.put("firstName", c.getClient().getFirstName());
                    simpleClient.put("lastName", c.getClient().getLastName());
                    simpleClient.put("email", c.getClient().getEmail());
                    simpleClient.put("telephone", c.getClient().getTelephone());
                    simpleComplaint.put("client", simpleClient);
                }

                simpleComplaints.add(simpleComplaint);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Liste des réclamations");
            response.put("count", simpleComplaints.size());
            response.put("complaints", simpleComplaints);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    // ComplaintController.java

    // Dans ComplaintController.java, modifiez la méthode getComplaintsByClient :
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getComplaintsByClient(@PathVariable Long clientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Récupérer le principal correctement
        Object principal = auth.getPrincipal();

        Long currentUserId = null;

        if (principal instanceof CustomUserPrincipal) {
            CustomUserPrincipal customUser = (CustomUserPrincipal) principal;
            currentUserId = customUser.getId();  // Supposons que votre CustomUserPrincipal a une méthode getId()
        } else if (principal instanceof User) {
            User user = (User) principal;
            currentUserId = user.getId();
        } else {
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));
        }

        // Vérifier que l'utilisateur ne voit que ses propres réclamations
        if (!currentUserId.equals(clientId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));
        }

        List<Complaint> complaints = complaintRepository.findByClient_Id(clientId);
        return ResponseEntity.ok(complaints);
    }
    // Ajoutez ces méthodes après la méthode getComplaintsByClient

    /**
     * RÉCUPÉRER les réclamations pour l'agent d'assurance connecté
     */
    /**
     * RÉCUPÉRER les réclamations pour l'agent d'assurance connecté
     */
    @GetMapping("/agent-assurance/complaints")
    @PreAuthorize("hasRole('AGENT_ASSURANCE')")
    public ResponseEntity<?> getComplaintsForAgentAssurance() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            User agent = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

            // ✅ Utilisez le bon nom de méthode avec underscore
            List<Complaint> complaints = complaintRepository.findByAgentAssurance_Id(agent.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Réclamations pour agent d'assurance");
            response.put("count", complaints.size());
            response.put("complaints", complaints);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * RÉCUPÉRER les réclamations pour l'agent financier connecté
     */
    @GetMapping("/agent-finance/complaints")
    @PreAuthorize("hasRole('AGENT_FINANCE')")
    public ResponseEntity<?> getComplaintsForAgentFinance() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            User agent = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Agent non trouvé"));

            // Log pour debug
            System.out.println("=== Agent Finance ===");
            System.out.println("Agent ID: " + agent.getId());
            System.out.println("Agent Email: " + agent.getEmail());

            List<Complaint> complaints = complaintRepository.findByAgentFinance_Id(agent.getId());

            System.out.println("Nombre de réclamations trouvées: " + complaints.size());

            // Alternative sans méthodes Repository supplémentaires
            List<Complaint> allComplaints = complaintRepository.findAll();
            long totalComplaints = allComplaints.size();

            long nullAgentCount = allComplaints.stream()
                    .filter(c -> c.getAgentFinance() == null)
                    .count();

            System.out.println("Total des réclamations en base: " + totalComplaints);
            System.out.println("Réclamations sans agent finance: " + nullAgentCount);

            // Afficher les IDs des réclamations
            System.out.println("IDs des réclamations trouvées:");
            complaints.forEach(c -> System.out.println("  - ID: " + c.getId() + ", Status: " + c.getStatus()));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Réclamations pour agent financier");
            response.put("count", complaints.size());
            response.put("complaints", complaints);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}