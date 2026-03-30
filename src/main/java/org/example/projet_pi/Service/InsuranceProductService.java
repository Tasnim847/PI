package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.InsuranceProductDTO;
import org.example.projet_pi.Mapper.InsuranceProductMapper;
import org.example.projet_pi.Repository.InsuranceProductRepository;
import org.example.projet_pi.entity.InsuranceProduct;
import org.example.projet_pi.entity.ProductStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class InsuranceProductService implements IInsuranceProductService {

    private final InsuranceProductRepository repository;

    @Override
    public InsuranceProductDTO addProduct(InsuranceProductDTO dto) {
        // 1️⃣ Vérifier que le produit n'existe pas déjà
        boolean exists = repository.findAll().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(dto.getName())
                        && p.getProductType().equalsIgnoreCase(dto.getProductType()));
        if (exists) {
            throw new RuntimeException("Le produit existe déjà dans la base de données.");
        }

        // 2️⃣ Validation stricte par type (méthier avancé)
        validateByType(dto);  // <-- exécuter ici

        // 3️⃣ Statut par défaut INACTIVE (avant calcul du statut réel)
        dto.setStatus("INACTIVE");

        // 4️⃣ Déterminer le statut selon le prix minimum
        ProductStatus status = determineStatus(dto);
        dto.setStatus(status.name());

        // 5️⃣ Sauvegarder le produit
        InsuranceProduct product = InsuranceProductMapper.toEntity(dto);
        product = repository.save(product);

        return InsuranceProductMapper.toDTO(product);
    }

    private ProductStatus determineStatus(InsuranceProductDTO dto) {
        switch (dto.getProductType().toUpperCase()) {
            case "AUTO":
                return dto.getBasePrice() >= 1000 ? ProductStatus.ACTIVE : ProductStatus.REFUSED;
            case "HABITATION":
                return dto.getBasePrice() >= 2000 ? ProductStatus.ACTIVE : ProductStatus.REFUSED;
            case "SANTE":
                return dto.getBasePrice() >= 500 ? ProductStatus.ACTIVE : ProductStatus.REFUSED;
            default:
                throw new RuntimeException("Type de produit inconnu");
        }
    }

    @Override
    public InsuranceProductDTO updateProduct(InsuranceProductDTO dto) {
        InsuranceProduct product = repository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getBasePrice() != null && dto.getBasePrice() > 0) product.setBasePrice(dto.getBasePrice());
        if (dto.getProductType() != null) product.setProductType(dto.getProductType());

        // 🔹 Si l'ADMIN a fourni un statut valide, on l'applique directement
        if (dto.getStatus() != null) {
            try {
                product.setStatus(ProductStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Statut invalide : " + dto.getStatus());
            }
        }

        product = repository.save(product);
        return InsuranceProductMapper.toDTO(product);
    }

    @Override
    public void deleteProduct(Long id) {
        repository.deleteById(id);
    }

    @Override
    public InsuranceProductDTO getProductById(Long id) {
        return InsuranceProductMapper.toDTO(repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé")));
    }

    @Override
    public List<InsuranceProductDTO> getAllProducts() {
        return repository.findAll().stream()
                .map(InsuranceProductMapper::toDTO)
                .toList();
    }

    // Dans InsuranceProductService
    @Override
    public List<InsuranceProductDTO> getActiveProducts() {
        return repository.findAll().stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE) // seuls produits actifs
                .map(InsuranceProductMapper::toDTO)
                .toList();
    }

    // ================= MÉTIER AVANCÉ : VALIDATION PAR TYPE =================
    private void validateByType(InsuranceProductDTO dto) {
        switch (dto.getProductType().toUpperCase()) {
            case "AUTO":
                if (dto.getBasePrice() < 1000) throw new RuntimeException("Le produit auto doit avoir un prix ≥ 1000");
                break;
            case "HABITATION":
                if (dto.getBasePrice() < 2000) throw new RuntimeException("Le produit habitation doit avoir un prix ≥ 2000");
                break;
            case "SANTE":
                if (dto.getBasePrice() < 500) throw new RuntimeException("Le produit santé doit avoir un prix ≥ 500");
                break;
            default:
                throw new RuntimeException("Type de produit inconnu");
        }
    }

    public InsuranceProductDTO changeProductStatus(Long productId, String statusStr) {
        InsuranceProduct product = repository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        try {
            ProductStatus status = ProductStatus.valueOf(statusStr.toUpperCase());
            product.setStatus(status);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide : " + statusStr);
        }

        product = repository.save(product);
        return InsuranceProductMapper.toDTO(product);
    }
}