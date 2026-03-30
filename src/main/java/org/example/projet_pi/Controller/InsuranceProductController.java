package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.InsuranceProductDTO;
import org.example.projet_pi.Service.IInsuranceProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/products")
public class InsuranceProductController {

    private final IInsuranceProductService insuranceProductService;

    // Seul l'ADMIN peut ajouter un produit
    @PostMapping("/addProduct")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addProduct(@RequestBody InsuranceProductDTO dto) {
        try {
            InsuranceProductDTO result = insuranceProductService.addProduct(dto);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Seul l'ADMIN peut modifier un produit
    @PutMapping("/updateProduct")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(@RequestBody InsuranceProductDTO dto) {
        try {
            InsuranceProductDTO result = insuranceProductService.updateProduct(dto);
            String message = "Produit mis à jour avec succès. Statut actuel : " + result.getStatus();
            return ResponseEntity.ok().body(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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