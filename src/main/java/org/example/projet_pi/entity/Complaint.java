package org.example.projet_pi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import java.util.Date;

@Entity
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long complaintId;

    private Date date;             // date de création
    private Date responseDate;     // date de traitement ou réponse

    private String message;
    private String response;
    private String status;

    @ManyToOne
    private Client client;

    @ManyToOne
    private AgentAssurance agentAssurance;

    @ManyToOne
    private AgentFinance agentFinance;

    // ---------------- Getters et Setters ----------------

    public Long getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(Date responseDate) {
        this.responseDate = responseDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public AgentAssurance getAgentAssurance() {
        return agentAssurance;
    }

    public void setAgentAssurance(AgentAssurance agentAssurance) {
        this.agentAssurance = agentAssurance;
    }

    public AgentFinance getAgentFinance() {
        return agentFinance;
    }

    public void setAgentFinance(AgentFinance agentFinance) {
        this.agentFinance = agentFinance;
    }
}