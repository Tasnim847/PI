package org.example.projet_pi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    private String name;
    private String type;
    private String filePath;

    private LocalDateTime uploadDate;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @ManyToOne
    private Client client;

    @ManyToOne
    private Claim claim;

    @ManyToOne
    private Credit credit;

    @ManyToOne
    private AgentAssurance verifiedByAssurance;

    @ManyToOne
    private AgentFinance verifiedByFinance;

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim claim) {
        this.claim = claim;
    }

    public Credit getCredit() {
        return credit;
    }

    public void setCredit(Credit credit) {
        this.credit = credit;
    }

    public AgentAssurance getVerifiedByAssurance() {
        return verifiedByAssurance;
    }

    public void setVerifiedByAssurance(AgentAssurance verifiedByAssurance) {
        this.verifiedByAssurance = verifiedByAssurance;
    }

    public AgentFinance getVerifiedByFinance() {
        return verifiedByFinance;
    }

    public void setVerifiedByFinance(AgentFinance verifiedByFinance) {
        this.verifiedByFinance = verifiedByFinance;
    }
}
