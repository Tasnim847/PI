package org.example.projet_pi.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.InsuranceProductDTO;
import org.example.projet_pi.Service.IInsuranceProductService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/products")
public class InsuranceProductController {

    private static final Logger logger = LoggerFactory.getLogger(InsuranceProductController.class);

    private final IInsuranceProductService insuranceProductService;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @PostMapping(value = "/addProduct", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addProduct(
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            InsuranceProductDTO dto = objectMapper.readValue(productJson, InsuranceProductDTO.class);
            InsuranceProductDTO result = insuranceProductService.addProduct(dto, imageFile);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    @PutMapping(value = "/updateProduct", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            InsuranceProductDTO dto = objectMapper.readValue(productJson, InsuranceProductDTO.class);
            InsuranceProductDTO result = insuranceProductService.updateProduct(dto, imageFile);
            return ResponseEntity.ok("Produit mis à jour avec succès. Statut actuel : " + result.getStatus());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    // Seul l'ADMIN peut supprimer un produit
    @DeleteMapping("/deleteProduct/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            insuranceProductService.deleteProduct(id);
            return ResponseEntity.ok("Produit supprimé avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Tous les utilisateurs authentifiés peuvent consulter un produit
    @GetMapping("/getProduct/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            InsuranceProductDTO result = insuranceProductService.getProductById(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Tous les utilisateurs authentifiés peuvent voir tous les produits
    @GetMapping("/allProduct")
    public ResponseEntity<?> getAllProducts() {
        try {
            List<InsuranceProductDTO> result = insuranceProductService.getAllProducts();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Dans InsuranceProductController

    // Tous les utilisateurs authentifiés (CLIENT et AGENT) peuvent voir les produits actifs
    @GetMapping("/activeProducts")
    public ResponseEntity<?> getActiveProducts() {
        try {
            List<InsuranceProductDTO> products = insuranceProductService.getActiveProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Dans InsuranceProductController.java
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) {
        try {
            // Le chemin complet vers le fichier
            Path imagePath = Paths.get("uploads/produits/", filename);
            byte[] imageBytes = Files.readAllBytes(imagePath);

            // Déterminer le type MIME
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(imageBytes);
        } catch (Exception e) {
            logger.error("Image non trouvée: {}", filename, e);
            return ResponseEntity.notFound().build();
        }
    }

    // Activer un produit
    @PutMapping("/activate/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateProduct(@PathVariable Long id) {
        try {
            InsuranceProductDTO updated = insuranceProductService.changeProductStatus(id, "ACTIVE");
            return ResponseEntity.ok("Produit activé avec succès : " + updated.getStatus());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Affecter/Modifier l'image d'un produit
    @PutMapping(value = "/{productId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignImageToProduct(
            @PathVariable Long productId,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        try {
            if (imageFile == null || imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Veuillez sélectionner une image");
            }

            InsuranceProductDTO result = insuranceProductService.assignImageToProduct(productId, imageFile);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    @DeleteMapping("/{productId}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeImageFromProduct(@PathVariable Long productId) {
        try {
            InsuranceProductDTO result = insuranceProductService.removeImageFromProduct(productId);
            return ResponseEntity.ok("Image supprimée avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Désactiver un produit
    @PutMapping("/deactivate/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateProduct(@PathVariable Long id) {
        try {
            InsuranceProductDTO updated = insuranceProductService.changeProductStatus(id, "INACTIVE");
            return ResponseEntity.ok("Produit désactivé avec succès : " + updated.getStatus());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}