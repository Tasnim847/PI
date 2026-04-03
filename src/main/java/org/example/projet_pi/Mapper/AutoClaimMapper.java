package org.example.projet_pi.Mapper;

import org.example.projet_pi.Dto.AutoClaimDetailsDTO;
import org.example.projet_pi.entity.AutoClaimDetails;

public class AutoClaimMapper {

    public static AutoClaimDetails toEntity(AutoClaimDetailsDTO dto) {
        if (dto == null) return null;

        AutoClaimDetails entity = new AutoClaimDetails();
        entity.setId(dto.getId());
        entity.setDriverA(dto.getDriverA());
        entity.setDriverB(dto.getDriverB());
        entity.setVehicleA(dto.getVehicleA());
        entity.setVehicleB(dto.getVehicleB());
        entity.setAccidentLocation(dto.getAccidentLocation());
        entity.setAccidentDate(dto.getAccidentDate());

        return entity;
    }

    public static AutoClaimDetailsDTO toDTO(AutoClaimDetails entity) {
        if (entity == null) return null;

        AutoClaimDetailsDTO dto = new AutoClaimDetailsDTO();
        dto.setId(entity.getId());
        dto.setDriverA(entity.getDriverA());
        dto.setDriverB(entity.getDriverB());
        dto.setVehicleA(entity.getVehicleA());
        dto.setVehicleB(entity.getVehicleB());
        dto.setAccidentLocation(entity.getAccidentLocation());
        dto.setAccidentDate(entity.getAccidentDate());

        return dto;
    }
}