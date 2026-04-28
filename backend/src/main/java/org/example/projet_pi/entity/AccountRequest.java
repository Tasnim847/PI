package org.example.projet_pi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_requests")
public class AccountRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    private AccountType requestedType;

    @Enumerated(EnumType.STRING)
    private AccountRequestStatus status;  // Changé de RequestStatus à AccountRequestStatus

    private String rejectionReason;

    private LocalDateTime requestDate;

    private LocalDateTime processedDate;

    @ManyToOne
    @JoinColumn(name = "processed_by_id")
    private AgentFinance processedBy;

    // Constructeurs
    public AccountRequest() {
        this.status = AccountRequestStatus.PENDING;  // Changé
        this.requestDate = LocalDateTime.now();
    }

    public AccountRequest(Client client, AccountType requestedType) {
        this();
        this.client = client;
        this.requestedType = requestedType;
    }

    // Getters
    public Long getId() { return id; }
    public Client getClient() { return client; }
    public AccountType getRequestedType() { return requestedType; }
    public AccountRequestStatus getStatus() { return status; }  // Changé
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getRequestDate() { return requestDate; }
    public LocalDateTime getProcessedDate() { return processedDate; }
    public AgentFinance getProcessedBy() { return processedBy; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setClient(Client client) { this.client = client; }
    public void setRequestedType(AccountType requestedType) { this.requestedType = requestedType; }
    public void setStatus(AccountRequestStatus status) { this.status = status; }  // Changé
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }
    public void setProcessedDate(LocalDateTime processedDate) { this.processedDate = processedDate; }
    public void setProcessedBy(AgentFinance processedBy) { this.processedBy = processedBy; }
}