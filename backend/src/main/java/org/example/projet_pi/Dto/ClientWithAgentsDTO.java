package org.example.projet_pi.Dto;

import lombok.Data;

import java.util.Date;

@Data
public class ClientWithAgentsDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String telephone;
    private String role;
    private String photo;
    private Double annualIncome;
    private Date createdAt;

    // 🔥 CHANGEMENT: Utiliser Long pour l'ID seulement
    private Long agentFinanceId;
    private String agentFinanceName;
    private Long agentAssuranceId;
    private String agentAssuranceName;
}