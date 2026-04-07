package org.example.projet_pi.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ComplaintSearchDTO {
    private String status;
    private String keyword;
    private Long clientId;
    private Long agentAssuranceId;
    private Long agentFinanceId;
    private Date dateDebut;
    private Date dateFin;
}