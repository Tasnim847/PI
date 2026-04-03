package org.example.projet_pi.Mapper;

import org.example.projet_pi.Dto.HealthClaimDetailsDTO;
import org.example.projet_pi.entity.HealthClaimDetails;

public class HealthClaimMapper {

    public static HealthClaimDetails toEntity(HealthClaimDetailsDTO dto) {
        if (dto == null) return null;

        HealthClaimDetails entity = new HealthClaimDetails();
        entity.setId(dto.getId());
        entity.setPatientName(dto.getPatientName());
        entity.setHospitalName(dto.getHospitalName());
        entity.setDoctorName(dto.getDoctorName());
        entity.setMedicalCost(dto.getMedicalCost());
        entity.setIllnessType(dto.getIllnessType());

        return entity;
    }

    public static HealthClaimDetailsDTO toDTO(HealthClaimDetails entity) {
        if (entity == null) return null;

        HealthClaimDetailsDTO dto = new HealthClaimDetailsDTO();
        dto.setId(entity.getId());
        dto.setPatientName(entity.getPatientName());
        dto.setHospitalName(entity.getHospitalName());
        dto.setDoctorName(entity.getDoctorName());
        dto.setMedicalCost(entity.getMedicalCost());
        dto.setIllnessType(entity.getIllnessType());

        return dto;
    }
}