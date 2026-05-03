package org.example.projet_pi.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projet_pi.Dto.ClaimDTO;
import org.example.projet_pi.Dto.CompensationDetailsDTO;
import org.example.projet_pi.Dto.DocumentDTO;
import org.example.projet_pi.Service.ClaimService;
import org.example.projet_pi.Service.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/claims")
public class ClaimController {

    private final ClaimService claimService;
    private final ObjectMapper objectMapper;
    private final DocumentService documentService;

    // ⚠️ SUPPRIMER cette méthode pour éviter le conflit
    /*
    @PostMapping("/addClaim")
    public ClaimDTO addClaim(
            @RequestBody ClaimDTO claimDTO,
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.addClaim(claimDTO, currentUser.getUsername());
    }
    */

    @PutMapping("/updateClaim")
    public ClaimDTO updateClaim(
            @RequestBody ClaimDTO claimDTO,
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.updateClaim(claimDTO, currentUser.getUsername());
    }

    @DeleteMapping("/deleteClaim/{id}")
    public void deleteClaim(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        claimService.deleteClaim(id, currentUser.getUsername());
    }

    @GetMapping("/getClaim/{id}")
    public ClaimDTO getClaimById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.getClaimById(id, currentUser.getUsername());
    }

    @GetMapping("/allClaim")
    public List<ClaimDTO> getAllClaims(
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.getAllClaims(currentUser.getUsername());
    }

    @PostMapping("/approve/{id}")
    public ClaimDTO approveClaim(
            @PathVariable Long id,
            @RequestParam(required = false) Double approvedAmount,
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.approveClaim(id, approvedAmount, currentUser.getUsername());
    }

    @PostMapping("/reject/{id}")
    public ClaimDTO rejectClaim(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.rejectClaim(id, reason, currentUser.getUsername());
    }

    @GetMapping("/calculate-compensation/{id}")
    public CompensationDetailsDTO calculateCompensation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        return claimService.getCompensationDetails(id);
    }

    @GetMapping("/calculate-compensation/{id}/text")
    public String calculateCompensationText(@PathVariable Long id) {
        CompensationDetailsDTO details = claimService.getCompensationDetails(id);
        return String.format(
                "Claim %d: %s\n" +
                        "   Montant réclamé: %.2f DT\n" +
                        "   Montant approuvé: %.2f DT\n" +
                        "   Franchise client: %.2f DT\n" +
                        "   Montant assurance: %.2f DT",
                details.getClaimId(),
                details.getStatus(),
                details.getClaimedAmount(),
                details.getApprovedAmount(),
                details.getFranchise(),
                details.getInsurancePayment()
        );
    }

    @GetMapping("/fraud/{id}")
    public String fraud(@PathVariable Long id) {
        return claimService.isFraudulent(id)
                ? "⚠️ Fraud detected"
                : "✅ No fraud";
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return claimService.getStats();
    }

    @GetMapping("/search")
    public List<ClaimDTO> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max) {
        return claimService.search(status, min, max);
    }

    @GetMapping("/recommendation/{id}")
    public String recommendation(@PathVariable Long id) {
        return claimService.getRecommendation(id);
    }

    @GetMapping("/prediction/{clientId}")
    public double prediction(@PathVariable Long clientId) {
        return claimService.predictClientCost(clientId);
    }

    @PostMapping("/claim/{claimId}/auto-decision")
    public ResponseEntity<ClaimDTO> autoDecision(@PathVariable Long claimId) {
        ClaimDTO result = claimService.decideClaimAutomatically(claimId);
        return ResponseEntity.ok(result);
    }

    // ✅ NOUVEAU ENDPOINT - Comme addProduct
    @PostMapping(value = "/addClaim", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addClaim(
            @RequestPart("claim") String claimJson,
            @RequestPart(value = "documents", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            // Convertir le JSON en ClaimDTO
            ClaimDTO claimDTO = objectMapper.readValue(claimJson, ClaimDTO.class);

            log.info("Réception de la réclamation: {}", claimDTO);
            log.info("Nombre de fichiers reçus: {}", files != null ? files.size() : 0);

            // Convertir les fichiers en DocumentDTO
            if (files != null && !files.isEmpty()) {
                List<DocumentDTO> documents = new ArrayList<>();
                for (MultipartFile file : files) {
                    DocumentDTO docDTO = new DocumentDTO();
                    docDTO.setName(file.getOriginalFilename());
                    docDTO.setType(file.getContentType());

                    // Sauvegarder le fichier
                    String filePath = saveFile(file);
                    docDTO.setFilePath(filePath);
                    documents.add(docDTO);
                }
                claimDTO.setDocuments(documents);
            }

            ClaimDTO result = claimService.addClaim(claimDTO, currentUser.getUsername());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Erreur lors de l'ajout de la réclamation", e);
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    // ClaimController.java - Version corrigée pour stocker le chemin absolu
    private String saveFile(MultipartFile file) {
        try {
            // Créer le dossier uploads/claims/ dans le répertoire de l'application
            String uploadDir = System.getProperty("user.dir") + "/uploads/claims/";
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            // Créer le dossier s'il n'existe pas
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Dossier créé: {}", uploadPath);
            }

            // Vérifier la taille (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("Le fichier ne doit pas dépasser 10MB");
            }

            // Générer un nom unique
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            // Chemin complet du fichier
            Path filePath = uploadPath.resolve(fileName);

            // Sauvegarde physique
            Files.copy(file.getInputStream(), filePath);

            log.info("Fichier sauvegardé: {}", filePath);

            // Retourner le chemin ABSOLU pour la BDD
            return filePath.toString();

        } catch (IOException e) {
            log.error("Erreur lors de la sauvegarde du fichier", e);
            throw new RuntimeException("Erreur lors de la sauvegarde du fichier: " + e.getMessage());
        }
    }

    // Ajoutez cette méthode dans ClaimController.java

    @GetMapping("/download/{documentId}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            // Récupérer le document depuis la BDD
            DocumentDTO document = documentService.getDocumentById(documentId, currentUser.getUsername());

            String filePath = document.getFilePath();
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                // Essayer le chemin relatif
                String alternativePath = System.getProperty("user.dir") + "/" + filePath;
                path = Paths.get(alternativePath);

                if (!Files.exists(path)) {
                    return ResponseEntity.notFound().build();
                }
            }

            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du document {}", documentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Ajoutez cette méthode pour lister les fichiers (debug)
    @GetMapping("/debug/files")
    public ResponseEntity<Map<String, Object>> debugFiles() {
        String uploadDir = System.getProperty("user.dir") + "/uploads/claims/";
        Path uploadPath = Paths.get(uploadDir);

        Map<String, Object> result = new HashMap<>();
        result.put("uploadDirectory", uploadPath.toString());
        result.put("directoryExists", Files.exists(uploadPath));

        List<String> files = new ArrayList<>();
        if (Files.exists(uploadPath)) {
            try {
                files = Files.list(uploadPath)
                        .map(Path::toString)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                files.add("Error listing files: " + e.getMessage());
            }
        }
        result.put("files", files);

        return ResponseEntity.ok(result);
    }
}