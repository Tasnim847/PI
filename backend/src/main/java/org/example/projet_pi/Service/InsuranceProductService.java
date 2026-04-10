package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.InsuranceProductDTO;
import org.example.projet_pi.Mapper.InsuranceProductMapper;
import org.example.projet_pi.Repository.InsuranceProductRepository;
import org.example.projet_pi.entity.InsuranceProduct;
import org.example.projet_pi.entity.ProductStatus;
import org.example.projet_pi.entity.ProductType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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

        // 🔹 Gérer le fichier image - Stocker le chemin original sans copier
        if (imageFile != null && !imageFile.isEmpty()) {
            String originalPath = saveOriginalImagePath(imageFile);
            dto.setImageUrl(originalPath); // Stocke le chemin original
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
                return ProductStatus.INACTIVE;
        }
    }

    @Override
    public InsuranceProductDTO updateProduct(InsuranceProductDTO dto, MultipartFile imageFile) {
        InsuranceProduct product = repository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

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

        // 🔹 Mettre à jour l'image si une nouvelle est fournie
        if (imageFile != null && !imageFile.isEmpty()) {
            String originalPath = saveOriginalImagePath(imageFile);
            product.setImageUrl(originalPath);
        }

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

    /**
     * Nouvelle méthode : Sauvegarde le chemin original de l'image sans copier le fichier
     */
    private String saveOriginalImagePath(MultipartFile file) {
        try {
            // Récupérer le nom original du fichier
            String originalFilename = file.getOriginalFilename();

            // Option 1: Utiliser un chemin temporaire ou un dossier spécifique
            // String UPLOAD_DIR = "C:/uploads/produits/"; // Dossier spécifique sur votre disque

            // Option 2: Pour l'instant, on va créer un dossier local
            String UPLOAD_DIR = "uploads/produits/";
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();

            // Créer le dossier s'il n'existe pas
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Dossier créé: {}", uploadPath.toString());
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

            // Générer un nom unique avec UUID pour éviter les conflits
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // Sauvegarder physiquement le fichier (obligatoire car le frontend ne peut pas accéder au chemin original)
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            logger.info("Image sauvegardée: {} -> {}", filename, filePath.toString());

            // Retourner le chemin ou le nom du fichier
            // Option A: Retourner seulement le nom (recommandé)
            return filename;

            // Option B: Retourner le chemin complet (si vous voulez)
            // return filePath.toString();

        } catch (IOException e) {
            logger.error("Erreur lors de l'upload de l'image", e);
            throw new RuntimeException("Erreur lors de l'enregistrement de l'image: " + e.getMessage());
        }
    }

    private void deleteImageFile(String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String filename = imageUrl;
                if (filename.contains("/") || filename.contains("\\")) {
                    filename = filename.replace("\\", "/");
                    filename = filename.substring(filename.lastIndexOf("/") + 1);
                }
                Path imagePath = Paths.get("uploads/produits/", filename);
                if (Files.exists(imagePath)) {
                    Files.delete(imagePath);
                    logger.info("Image supprimée: {}", filename);
                }
            }
        } catch (IOException e) {
            logger.warn("Impossible de supprimer l'image: {}", e.getMessage());
        }
    }

    @Override
    public void deleteProduct(Long id) {
        InsuranceProduct product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

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

    @Override
    public List<InsuranceProductDTO> getActiveProducts() {
        return repository.findAll().stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .map(InsuranceProductMapper::toDTO)
                .toList();
    }

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
            case HOME:
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

        InsuranceProduct product = repository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'ID: " + productId));

        if (imageFile == null || imageFile.isEmpty()) {
            throw new RuntimeException("Veuillez sélectionner une image");
        }

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            deleteImageFile(product.getImageUrl());
            logger.info("Ancienne image supprimée: {}", product.getImageUrl());
        }

        String imageUrl = saveOriginalImagePath(imageFile);
        product.setImageUrl(imageUrl);

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