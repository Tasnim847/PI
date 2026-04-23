package org.example.projet_pi.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ComplaintDTO {
    private Long id;
    private String status;
    private String message;
    private String phone;  // ← AJOUTER CETTE LIGNE
    private LocalDateTime claimDate;
    private LocalDateTime resolutionDate;
    private Long clientId;
    private Long agentAssuranceId;
    private Long agentFinanceId;
}