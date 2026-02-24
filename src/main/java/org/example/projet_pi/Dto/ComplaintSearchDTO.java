package org.example.projet_pi.Dto;

import lombok.Data;

import java.util.Date;

@Data
public class ComplaintSearchDTO {

    private String status;
    private String keyword;
    private Long clientId;
    private Long agentAssuranceId;
    private Long agentFinanceId;
    private Date dateDebut;
    private Date dateFin;
}