package org.example.projet_pi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.Id;

import java.util.Date;

@Entity
public class Complaint {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long complaintId;


    private Date date;

    private String message;
    private String response;
    private String status;

    @ManyToOne
    private Client client;

    @ManyToOne
    private AgentAssurance agentAssurance;

    public Long getComplaintId() {
        return complaintId;
    }

    public Date getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDate(Date date) {
        this.date = date;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }

    @ManyToOne
    private AgentFinance agentFinance;
}

