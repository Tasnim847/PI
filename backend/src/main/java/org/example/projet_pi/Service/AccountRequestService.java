package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.AccountRequestDTO;
import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.Repository.AccountRequestRepository;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountRequestService {

    private final AccountRequestRepository accountRequestRepository;
    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;

    public AccountRequestService(AccountRequestRepository accountRequestRepository,
                                 AccountRepository accountRepository,
                                 ClientRepository clientRepository) {
        this.accountRequestRepository = accountRequestRepository;
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
    }

    // 🔹 CLIENT : Créer une demande de compte
    @Transactional
    public AccountRequest createRequest(Long clientId, AccountType type) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        // Vérifier si le client a déjà un compte actif de ce type
        boolean hasExistingAccount = accountRepository.findByClientId(clientId)
                .stream()
                .anyMatch(a -> a.getType() == type && "ACTIVE".equals(a.getStatus()));

        if (hasExistingAccount) {
            throw new RuntimeException("Vous avez déjà un compte " + type + " actif");
        }

        AccountRequest request = new AccountRequest(client, type);
        return accountRequestRepository.save(request);
    }

    // 🔹 CLIENT : Récupérer ses demandes
    public List<AccountRequestDTO> getClientRequests(Long clientId) {
        return accountRequestRepository.findByClientId(clientId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 AGENT_FINANCE : Récupérer toutes les demandes en attente
    public List<AccountRequestDTO> getPendingRequests() {
        return accountRequestRepository.findByStatus(AccountRequestStatus.PENDING)  // Changé
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 AGENT_FINANCE : Approuver une demande
    @Transactional
    public Account approveRequest(Long requestId, Long agentId) {
        AccountRequest request = accountRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (request.getStatus() != AccountRequestStatus.PENDING) {  // Changé
            throw new RuntimeException("Cette demande a déjà été traitée");
        }

        // Créer le compte
        Account newAccount = new Account();
        newAccount.setType(request.getRequestedType());
        newAccount.setBalance(0.0);
        newAccount.setStatus("ACTIVE");
        newAccount.setClient(request.getClient());

        // Valeurs par défaut pour l'agent (peuvent être modifiées après)
        newAccount.setDailyLimit(2000.0);
        newAccount.setMonthlyLimit(20000.0);
        newAccount.setDailyTransferLimit(10000.0);

        Account savedAccount = accountRepository.save(newAccount);

        // Mettre à jour la demande
        request.setStatus(AccountRequestStatus.APPROVED);  // Changé
        request.setProcessedDate(java.time.LocalDateTime.now());

        accountRequestRepository.save(request);

        return savedAccount;
    }

    // 🔹 AGENT_FINANCE : Rejeter une demande
    @Transactional
    public void rejectRequest(Long requestId, Long agentId, String reason) {
        AccountRequest request = accountRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (request.getStatus() != AccountRequestStatus.PENDING) {  // Changé
            throw new RuntimeException("Cette demande a déjà été traitée");
        }

        request.setStatus(AccountRequestStatus.REJECTED);  // Changé
        request.setRejectionReason(reason);
        request.setProcessedDate(java.time.LocalDateTime.now());

        accountRequestRepository.save(request);
    }

    private AccountRequestDTO convertToDTO(AccountRequest request) {
        String clientName = request.getClient().getFirstName() + " " + request.getClient().getLastName();
        return new AccountRequestDTO(
                request.getId(),
                request.getClient().getId(),
                clientName,
                request.getRequestedType(),
                request.getStatus(),  // Changé (maintenant AccountRequestStatus)
                request.getRejectionReason(),
                request.getRequestDate(),
                request.getProcessedDate()
        );
    }
}