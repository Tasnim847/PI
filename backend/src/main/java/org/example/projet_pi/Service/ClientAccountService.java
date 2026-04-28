package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.ClientAccountDTO;
import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.entity.Account;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientAccountService {

    private final AccountRepository accountRepository;

    public ClientAccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // 🔹 Récupérer tous les comptes d'un client
    public List<ClientAccountDTO> getClientAccounts(Long clientId) {
        return accountRepository.findByClientId(clientId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer un compte par son RIP (avec vérification propriété)
    public Account getAccountByRip(String rip, Long clientId) {
        Account account = accountRepository.findByRip(rip)
                .orElseThrow(() -> new RuntimeException("Compte non trouvé avec RIP: " + rip));

        if (!account.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Ce compte ne vous appartient pas");
        }

        return account;
    }

    // 🔹 Récupérer un compte par son ID (avec vérification)
    public Account getClientAccountById(Long accountId, Long clientId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Compte non trouvé"));

        if (!account.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Ce compte ne vous appartient pas");
        }

        return account;
    }

    private ClientAccountDTO convertToDTO(Account account) {
        return new ClientAccountDTO(
                account.getAccountId(),
                account.getRip(),
                account.getBalance(),
                account.getType(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getDailyLimit(),
                account.getMonthlyLimit(),
                account.getDailyTransferLimit()
        );
    }
}