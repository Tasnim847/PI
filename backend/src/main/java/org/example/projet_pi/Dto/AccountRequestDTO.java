package org.example.projet_pi.Dto;

import org.example.projet_pi.entity.AccountType;
import org.example.projet_pi.entity.AccountRequestStatus;  // Changé
import java.time.LocalDateTime;

public class AccountRequestDTO {
    private Long id;
    private Long clientId;
    private String clientName;
    private AccountType requestedType;
    private AccountRequestStatus status;  // Changé
    private String rejectionReason;
    private LocalDateTime requestDate;
    private LocalDateTime processedDate;

    public AccountRequestDTO(Long id, Long clientId, String clientName,
                             AccountType requestedType, AccountRequestStatus status,  // Changé
                             String rejectionReason, LocalDateTime requestDate,
                             LocalDateTime processedDate) {
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
        this.requestedType = requestedType;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.requestDate = requestDate;
        this.processedDate = processedDate;
    }

    // Getters
    public Long getId() { return id; }
    public Long getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public AccountType getRequestedType() { return requestedType; }
    public AccountRequestStatus getStatus() { return status; }  // Changé
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getRequestDate() { return requestDate; }
    public LocalDateTime getProcessedDate() { return processedDate; }
}