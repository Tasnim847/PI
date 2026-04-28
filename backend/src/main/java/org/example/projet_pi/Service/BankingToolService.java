package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.Repository.ComplaintRepository;
import org.example.projet_pi.Repository.TransactionRepository;
import org.example.projet_pi.entity.Account;
import org.example.projet_pi.entity.Complaint;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BankingToolService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ComplaintRepository complaintRepository;
    private final TransactionService transactionService;

    public BankingToolService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              ComplaintRepository complaintRepository,
                              TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.complaintRepository = complaintRepository;
        this.transactionService = transactionService;
    }

    public String getBalance(Long userId) {
        List<Account> accounts = accountRepository.findByClientId(userId);
        if (accounts.isEmpty()) return "0";
        double total = accounts.stream().mapToDouble(Account::getBalance).sum();
        return String.format("%.2f", total);
    }

    public List<Map<String, Object>> getRecentTransactions(Long userId, int limit) {
        List<Account> accounts = accountRepository.findByClientId(userId);
        if (accounts.isEmpty()) return List.of();

        Account mainAccount = accounts.get(0);
        return transactionRepository.findByAccountAccountId(mainAccount.getAccountId())
                .stream()
                .limit(limit)
                .map(tx -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", tx.getTransactionId());
                    map.put("date", tx.getDate().toString());
                    map.put("type", tx.getType());
                    map.put("amount", tx.getAmount());
                    map.put("description", tx.getDescription());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public String transfer(Long userId, String targetRip, double amount, String description) {
        List<Account> accounts = accountRepository.findByClientId(userId);
        if (accounts.isEmpty()) return "Vous n'avez pas de compte";

        try {
            return transactionService.transferByRip(
                    accounts.get(0).getRip(),
                    targetRip,
                    amount,
                    description != null ? description : "Virement via assistant IA",
                    userId
            );
        } catch (Exception e) {
            return "Erreur: " + e.getMessage();
        }
    }

    public String createComplaint(Long userId, String message) {
        try {
            // Récupérer le client
            List<Account> accounts = accountRepository.findByClientId(userId);
            if (accounts.isEmpty()) return "Client non trouvé";

            Account account = accounts.get(0);

            Complaint complaint = new Complaint();
            complaint.setClient(account.getClient());
            complaint.setMessage(message);
            complaint.setStatus("PENDING");
            complaint.setClaimDate(new Date());

            complaintRepository.save(complaint);
            return "✅ Votre réclamation a été enregistrée. Un conseiller vous contactera sous 48h.";
        } catch (Exception e) {
            return "❌ Erreur lors de l'enregistrement: " + e.getMessage();
        }
    }

    public String getAccountInfo(Long userId) {
        List<Account> accounts = accountRepository.findByClientId(userId);
        if (accounts.isEmpty()) return "Aucun compte trouvé";

        Account acc = accounts.get(0);
        return String.format("Compte %s - Solde: %.2f TND - RIP: %s",
                acc.getType(), acc.getBalance(), formatRIP(acc.getRip()));
    }

    private String formatRIP(String rip) {
        if (rip == null || rip.length() != 21) return rip;
        return String.format("%s %s %s %s",
                rip.substring(0, 5), rip.substring(5, 10),
                rip.substring(10, 15), rip.substring(15, 21));
    }
}