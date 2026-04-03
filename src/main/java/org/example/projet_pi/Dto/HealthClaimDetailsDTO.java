package org.example.projet_pi.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HealthClaimDetailsDTO {

    private Long id;

    private String patientName;
    private String hospitalName;
    private String doctorName;

    private Double medicalCost;
    private String illnessType;
}