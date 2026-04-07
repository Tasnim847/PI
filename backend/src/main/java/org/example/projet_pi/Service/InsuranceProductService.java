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
        boolean exists = repository.findAll().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(dto.getName())
                        && p.getProductType().equalsIgnoreCase(dto.getProductType()));
        if (exists) {
            throw new RuntimeException("Le produit existe déjà dans la base de données.");
        }

        // Statut par défaut INACTIVE
        dto.setStatus("INACTIVE");

        // Vérification couverture minimum
        ProductStatus status = determineStatus(dto);
        dto.setStatus(status.name());

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
        if (dto.getBasePrice() > 0) product.setBasePrice(dto.getBasePrice());
        if (dto.getProductType() != null) product.setProductType(dto.getProductType());

        // Statut mis à jour automatiquement
        ProductStatus status = determineStatus(InsuranceProductMapper.toDTO(product));
        product.setStatus(status);

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
}