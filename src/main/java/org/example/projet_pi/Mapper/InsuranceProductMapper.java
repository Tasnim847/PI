package org.example.projet_pi.Mapper;

import org.example.projet_pi.Dto.InsuranceProductDTO;
import org.example.projet_pi.entity.InsuranceProduct;
import org.example.projet_pi.entity.ProductStatus;
import org.example.projet_pi.entity.ProductType;

public class InsuranceProductMapper {

    // ==================== Entity -> DTO ====================
    public static InsuranceProductDTO toDTO(InsuranceProduct product) {
        if (product == null) return null;

        InsuranceProductDTO dto = new InsuranceProductDTO();
        dto.setProductId(product.getProductId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setBasePrice(product.getBasePrice());
        dto.setImageUrl(product.getImageUrl());
        dto.setProductType(product.getProductType());
        dto.setOtherProductType(product.getOtherType());
        dto.setStatus(product.getStatus() != null ? product.getStatus().name() : null);

        return dto;
    }

    // ==================== DTO -> Entity ====================
    public static InsuranceProduct toEntity(InsuranceProductDTO dto) {
        if (dto == null) return null;

        InsuranceProduct product = new InsuranceProduct();
        product.setProductId(dto.getProductId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setBasePrice(dto.getBasePrice());
        product.setImageUrl(dto.getImageUrl());

        // Pour OTHER : on met ProductType.OTHER dans productType
        // et le texte saisi dans otherType
        if (dto.getProductType() == ProductType.OTHER) {
            product.setProductType(ProductType.OTHER);
            product.setOtherType(dto.getOtherProductType());
        } else {
            product.setProductType(dto.getProductType());
            product.setOtherType(null);
        }

        // Statut par défaut si null
        product.setStatus(dto.getStatus() != null ? ProductStatus.valueOf(dto.getStatus()) : ProductStatus.INACTIVE);

        return product;
    }
}