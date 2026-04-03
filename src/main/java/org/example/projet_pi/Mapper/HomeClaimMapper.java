package org.example.projet_pi.Mapper;

import org.example.projet_pi.Dto.HomeClaimDetailsDTO;
import org.example.projet_pi.entity.HomeClaimDetails;

public class HomeClaimMapper {

    public static HomeClaimDetails toEntity(HomeClaimDetailsDTO dto) {
        if (dto == null) return null;

        HomeClaimDetails entity = new HomeClaimDetails();
        entity.setId(dto.getId());
        entity.setDamageType(dto.getDamageType());
        entity.setAddress(dto.getAddress());
        entity.setEstimatedLoss(dto.getEstimatedLoss());

        return entity;
    }

    public static HomeClaimDetailsDTO toDTO(HomeClaimDetails entity) {
        if (entity == null) return null;

        HomeClaimDetailsDTO dto = new HomeClaimDetailsDTO();
        dto.setId(entity.getId());
        dto.setDamageType(entity.getDamageType());
        dto.setAddress(entity.getAddress());
        dto.setEstimatedLoss(entity.getEstimatedLoss());

        return dto;
    }
}