package org.example.projet_pi.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.InsuranceProductDTO;
import org.example.projet_pi.Mapper.InsuranceProductMapper;
import org.example.projet_pi.Repository.InsuranceProductRepository;
import org.example.projet_pi.entity.InsuranceProduct;
import org.example.projet_pi.entity.ProductStatus;
import org.example.projet_pi.entity.ProductType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@AllArgsConstructor
public class InsuranceProductService implements IInsuranceProductService {

    private static final Logger logger = LoggerFactory.getLogger(InsuranceProductService.class);
    private final InsuranceProductRepository repository;

    @Override
    public InsuranceProductDTO addProduct(InsuranceProductDTO dto, MultipartFile imageFile) {
        // Vérifier que le produit n'existe pas déjà
        boolean exists = repository.findAll().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(dto.getName()) &&
                        p.getProductType() != null && dto.getProductType() != null &&
                        p.getProductType() == dto.getProductType());
        if (exists) {
            throw new RuntimeException("Le produit existe déjà dans la base de données.");
        }

        validateByType(dto);
        dto.setStatus("INACTIVE");

        ProductStatus status = ProductStatus.INACTIVE;
        if (dto.getProductType() != null && dto.getProductType() != ProductType.OTHER) {
            status = determineStatus(dto);
        } else {
            status = ProductStatus.ACTIVE;
        }
        dto.setStatus(status.name());

        // 🔹 Gérer le fichier image
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = saveImageFile(imageFile);
            dto.setImageUrl(imageUrl);
        }

        InsuranceProduct product = InsuranceProductMapper.toEntity(dto);
        product = repository.save(product);

        return InsuranceProductMapper.toDTO(product);
    }

    private ProductStatus determineStatus(InsuranceProductDTO dto) {
        switch (dto.getProductType()) {
            case AUTO:
                return dto.getBasePrice() >= 1000 ? ProductStatus.ACTIVE : ProductStatus.REFUSED;
            case LIFE:
                return dto.getBasePrice() >= 2000 ? ProductStatus.ACTIVE : ProductStatus.REFUSED;
            case HEALTH:
                return dto.getBasePrice() >= 500 ? ProductStatus.ACTIVE : ProductStatus.REFUSED;
            default:
                return ProductStatus.INACTIVE; // OTHER ou autre type
        }
    }

    @Override
    public InsuranceProductDTO updateProduct(InsuranceProductDTO dto, MultipartFile imageFile) {
        InsuranceProduct product = repository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        // Sauvegarder l'ancienne URL de l'image pour suppression
        String oldImageUrl = product.getImageUrl();

        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getBasePrice() != null && dto.getBasePrice() > 0) product.setBasePrice(dto.getBasePrice());

        if (dto.getProductType() != null) {
            if (dto.getProductType() == ProductType.OTHER) {
                if (dto.getOtherProductType() == null || dto.getOtherProductType().isBlank()) {
                    throw new RuntimeException("Veuillez fournir le nom du type pour 'Other'");
                }
                product.setOtherType(dto.getOtherProductType());
                product.setProductType(ProductType.OTHER);
            } else {
                product.setProductType(dto.getProductType());
                product.setOtherType(null);
            }
        }

        // 🔹 Gérer le fichier image - Supprimer l'ancienne si nouvelle image
        if (imageFile != null && !imageFile.isEmpty()) {
            // Supprimer l'ancienne image si elle existe
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                deleteImageFile(oldImageUrl);
            }
            // Sauvegarder la nouvelle image
            String imageUrl = saveImageFile(imageFile);
            product.setImageUrl(imageUrl);
        }
        // Si aucune nouvelle image, garder l'ancienne (ne rien faire)

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

    // Nouvelle méthode pour supprimer l'image physique
    private void deleteImageFile(String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Path imagePath = Paths.get(imageUrl);
                if (Files.exists(imagePath)) {
                    Files.delete(imagePath);
                    logger.info("Ancienne image supprimée: {}", imageUrl);
                }
            }
        } catch (IOException e) {
            logger.warn("Impossible de supprimer l'ancienne image: {}", e.getMessage());
            // Ne pas bloquer la mise à jour si la suppression échoue
        }
    }

    // Méthode utilitaire pour enregistrer l'image et retourner le chemin relatif
    private String saveImageFile(MultipartFile file) {
        try {
            String UPLOAD_DIR = "uploads/produits/";
            Path uploadPath = Paths.get(UPLOAD_DIR);

            // Créer le dossier s'il n'existe pas
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Dossier créé: {}", UPLOAD_DIR);
            }

            // Vérifier que le fichier est une image
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Le fichier doit être une image");
            }

            // Vérifier la taille (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("L'image ne doit pas dépasser 5MB");
            }

            // Générer un nom unique avec UUID
            String extension = "";
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // Sauvegarder le fichier
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            String imageUrl = UPLOAD_DIR + filename;
            logger.info("Image sauvegardée: {}", imageUrl);

            return imageUrl;

        } catch (IOException e) {
            logger.error("Erreur lors de l'upload de l'image", e);
            throw new RuntimeException("Erreur lors de l'enregistrement de l'image: " + e.getMessage());
        }
    }

    @Override
    public void deleteProduct(Long id) {
        InsuranceProduct product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        // Supprimer l'image associée
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            deleteImageFile(product.getImageUrl());
        }

        repository.deleteById(id);
        logger.info("Produit et son image supprimés avec succès: {}", id);
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
        if (dto.getProductType() == null) {
            throw new RuntimeException("Le type de produit est obligatoire");
        }

        switch (dto.getProductType()) {
            case AUTO:
                if (dto.getBasePrice() < 1000) throw new RuntimeException("Le produit auto doit avoir un prix ≥ 1000");
                break;
            case LIFE:
                if (dto.getBasePrice() < 2000) throw new RuntimeException("Le produit vie doit avoir un prix ≥ 2000");
                break;
            case HEALTH:
                if (dto.getBasePrice() < 500) throw new RuntimeException("Le produit santé doit avoir un prix ≥ 500");
                break;
            case HOME:      // <-- Ajouté ici
                if (dto.getBasePrice() < 800) throw new RuntimeException("Le produit habitation doit avoir un prix ≥ 800");
                break;
            case OTHER:
                if (dto.getOtherProductType() == null || dto.getOtherProductType().isBlank()) {
                    throw new RuntimeException("Vous devez préciser le type de produit pour OTHER");
                }
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

    @Override
    public InsuranceProductDTO assignImageToProduct(Long productId, MultipartFile imageFile) {
        logger.info("Affectation d'une image au produit ID: {}", productId);

        // Vérifier que le produit existe
        InsuranceProduct product = repository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'ID: " + productId));

        // Vérifier que l'image n'est pas vide
        if (imageFile == null || imageFile.isEmpty()) {
            throw new RuntimeException("Veuillez sélectionner une image");
        }

        // Supprimer l'ancienne image si elle existe
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            deleteImageFile(product.getImageUrl());
            logger.info("Ancienne image supprimée: {}", product.getImageUrl());
        }

        // Sauvegarder la nouvelle image
        String imageUrl = saveImageFile(imageFile);
        product.setImageUrl(imageUrl);

        // Sauvegarder le produit
        product = repository.save(product);
        logger.info("Image affectée avec succès au produit ID: {}", productId);

        return InsuranceProductMapper.toDTO(product);
    }

    @Override
    public InsuranceProductDTO removeImageFromProduct(Long productId) {
        logger.info("Suppression de l'image du produit ID: {}", productId);

        InsuranceProduct product = repository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'ID: " + productId));

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            deleteImageFile(product.getImageUrl());
            product.setImageUrl(null);
            product = repository.save(product);
            logger.info("Image supprimée du produit ID: {}", productId);
        }

        return InsuranceProductMapper.toDTO(product);
    }
}