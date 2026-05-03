package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.ClientDTO;
import org.example.projet_pi.Dto.InsuranceContractDTO;
import org.example.projet_pi.Dto.RiskClaimDTO;
import org.example.projet_pi.Mapper.InsuranceContractMapper;
import org.example.projet_pi.Mapper.RiskClaimMapper;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Repository.UserRepository;
import org.example.projet_pi.Service.IInsuranceContractService;
import org.example.projet_pi.Service.PdfGenerationService;
import org.example.projet_pi.entity.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/contrats")
public class InsuranceContractController {

    private final IInsuranceContractService contractService;
    private final InsuranceContractRepository contractRepository;
    private final PdfGenerationService pdfGenerationService;
    private final UserRepository userRepository;

    @PostMapping("/addCont")
    public InsuranceContractDTO addContract(
            @RequestBody InsuranceContractDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {

        User user = userRepository.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!(user instanceof Client client)) {
            throw new RuntimeException("Seul un client peut créer un contrat");
        }

        ClientDTO clientDTO = new ClientDTO();
        clientDTO.setId(client.getId());
        clientDTO.setFirstName(client.getFirstName());
        clientDTO.setLastName(client.getLastName());
        clientDTO.setEmail(client.getEmail());
        clientDTO.setTelephone(client.getTelephone());

        dto.setClient(clientDTO);

        return contractService.addContract(dto, currentUser.getUsername());
    }

    @PutMapping("/updateCont/{id}")
    public InsuranceContractDTO updateContract(
            @PathVariable Long id,
            @RequestBody InsuranceContractDTO dto,
            @AuthenticationPrincipal UserDetails currentUser) {
        return contractService.updateContract(id, dto, currentUser.getUsername());
    }

    @DeleteMapping("/deleteCont/{id}")
    public ResponseEntity<?> deleteContract(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        contractService.deleteContract(id, currentUser.getUsername());
        return ResponseEntity.ok("Contrat supprimé avec succès");
    }

    @GetMapping("/getCont/{id}")
    public InsuranceContractDTO getContractById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return contractService.getContractById(id, currentUser.getUsername());
    }

    @GetMapping("/allCont")
    public List<InsuranceContractDTO> getAllContracts(
            @AuthenticationPrincipal UserDetails currentUser) {
        return contractService.getAllContracts(currentUser.getUsername());
    }

    @GetMapping("/myContracts")
    public List<InsuranceContractDTO> getMyContracts(Authentication authentication) {
        String email = authentication.getName();
        return contractService.getContractsByClientEmail(email);
    }

    @PutMapping("/activate/{id}")
    public ResponseEntity<?> activateContract(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {

        System.out.println("=== ACTIVATION CONTRAT ===");
        System.out.println("ID contrat: " + id);
        System.out.println("Utilisateur: " + currentUser.getUsername());
        System.out.println("Rôles: " + currentUser.getAuthorities());

        try {
            InsuranceContractDTO activatedContract = contractService.activateContract(id, currentUser.getUsername());
            System.out.println("✅ Contrat activé avec succès: " + activatedContract.getContractId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Contrat activé avec succès",
                    "contract", activatedContract
            ));
        } catch (AccessDeniedException e) {
            System.err.println("❌ Accès refusé: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'activation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/activate-with-notification/{id}")
    public ResponseEntity<Map<String, Object>> activateContractWithNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {

        Map<String, Object> response = new HashMap<>();

        try {
            InsuranceContractDTO activatedContract = contractService.activateContract(id, currentUser.getUsername());
            response.put("success", true);
            response.put("message", "Contrat activé et notification envoyée");
            response.put("contract", activatedContract);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectContract(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> rejectionData,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            String reason = rejectionData != null ?
                    rejectionData.getOrDefault("reason", "Non spécifiée") :
                    "Non spécifiée";

            InsuranceContractDTO rejectedContract = contractService.rejectContract(id, currentUser.getUsername(), reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Contrat rejeté avec succès",
                    "contractId", id,
                    "status", "CANCELLED",
                    "reason", reason
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingContracts(
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            User user = userRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!(user instanceof AgentAssurance agent)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Seuls les agents peuvent voir les contrats en attente"));
            }

            List<InsuranceContractDTO> pendingContracts = contractRepository.findByAgentAssuranceId(agent.getId())
                    .stream()
                    .filter(c -> c.getStatus() == ContractStatus.INACTIVE)
                    .map(InsuranceContractMapper::toDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "agentId", agent.getId(),
                    "agentName", agent.getFirstName() + " " + agent.getLastName(),
                    "pendingCount", pendingContracts.size(),
                    "contracts", pendingContracts
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rejected")
    public ResponseEntity<?> getRejectedContracts(
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            User user = userRepository.findByEmail(currentUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<InsuranceContract> rejectedContracts;

            if (user instanceof AgentAssurance agent) {
                rejectedContracts = contractRepository.findByAgentAssuranceId(agent.getId())
                        .stream()
                        .filter(c -> c.getStatus() == ContractStatus.CANCELLED)
                        .collect(Collectors.toList());
            } else if (user instanceof Client client) {
                rejectedContracts = contractRepository.findByClient(client)
                        .stream()
                        .filter(c -> c.getStatus() == ContractStatus.CANCELLED)
                        .collect(Collectors.toList());
            } else {
                rejectedContracts = contractRepository.findByStatus(ContractStatus.CANCELLED);
            }

            List<InsuranceContractDTO> dtos = rejectedContracts.stream()
                    .map(InsuranceContractMapper::toDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", dtos.size(),
                    "contracts", dtos
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getContractStats(
            @AuthenticationPrincipal UserDetails currentUser) {

        List<InsuranceContractDTO> contracts = contractService.getAllContracts(currentUser.getUsername());

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", contracts.size());
        stats.put("active", contracts.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count());
        stats.put("inactive", contracts.stream().filter(c -> "INACTIVE".equals(c.getStatus())).count());
        stats.put("completed", contracts.stream().filter(c -> "COMPLETED".equals(c.getStatus())).count());
        stats.put("cancelled", contracts.stream().filter(c -> "CANCELLED".equals(c.getStatus())).count());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/risk")
    public ResponseEntity<?> getContractRisk(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            InsuranceContractDTO contractDTO = contractService.getContractById(id, currentUser.getUsername());

            InsuranceContract contract = contractRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé avec l'id: " + id));

            RiskClaim riskClaim = contract.getRiskClaim();
            if (riskClaim == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Aucune évaluation de risque trouvée", "contractId", id));
            }

            RiskClaimDTO riskDTO = RiskClaimMapper.toDTO(riskClaim);

            Map<String, Object> response = new HashMap<>();
            response.put("riskEvaluation", riskDTO);
            response.put("contractStatus", contract.getStatus());
            response.put("contractId", contract.getContractId());
            response.put("contractReference", "CTR-" + contract.getContractId());

            String recommendation;
            boolean canBeActivated;

            switch (riskClaim.getRiskLevel()) {
                case "HIGH":
                    recommendation = "Ce contrat présente un risque trop élevé. Il ne peut pas être activé.";
                    canBeActivated = false;
                    break;
                case "MEDIUM":
                    recommendation = "Ce contrat présente un risque modéré. Nécessite validation par un agent.";
                    canBeActivated = true;
                    break;
                case "LOW":
                    recommendation = "Ce contrat présente un risque faible. Peut être activé normalement.";
                    canBeActivated = true;
                    break;
                default:
                    recommendation = "Niveau de risque non déterminé.";
                    canBeActivated = false;
            }

            response.put("recommendation", recommendation);
            response.put("canBeActivated", canBeActivated);

            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accès non autorisé", "message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne", "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/download/pdf")
    public ResponseEntity<InputStreamResource> downloadContractPdf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            InsuranceContractDTO contractDTO = contractService.getContractById(id, currentUser.getUsername());

            InsuranceContract contract = contractRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Contrat non trouvé avec l'id: " + id));

            Client client = contract.getClient();
            AgentAssurance agent = contract.getAgentAssurance();
            List<Payment> payments = contract.getPayments();

            if (client == null) {
                throw new RuntimeException("Le contrat n'est pas associé à un client");
            }

            byte[] pdfContent = pdfGenerationService.generateContractPdf(contract, client, agent, payments);

            String filename = String.format("contrat_%d_%s.pdf", id, new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfContent.length)
                    .body(new InputStreamResource(new ByteArrayInputStream(pdfContent)));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== SYSTÈME (Admin only) ====================

    @PostMapping("/check-late-payments")
    public String checkAllLatePayments() {
        contractService.checkLatePayments();
        return "Vérification des retards effectuée";
    }

    @PostMapping("/check-late-payments/{id}")
    public String checkContractLatePayments(@PathVariable Long id) {
        contractService.checkContractLatePayments(id);
        return "Vérification du contrat " + id + " effectuée";
    }

    @PostMapping("/simulate-late-payments/{id}/{months}")
    public String simulateLatePayments(@PathVariable Long id, @PathVariable int months) {
        contractService.simulateLatePayments(id, months);
        return months + " mois de retard simulés pour le contrat " + id;
    }

    @PostMapping("/check-completed")
    public String checkCompletedContracts() {
        contractService.checkCompletedContracts();
        return "Vérification des contrats COMPLETED effectuée";
    }

    @PostMapping("/check-end-of-month")
    public String checkEndOfMonth() {
        contractService.checkEndOfMonthLatePayments();
        return "Vérification de fin de mois effectuée";
    }
}