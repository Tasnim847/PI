package org.example.projet_pi.Controller;

import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.Repository.TransactionRepository;
import org.example.projet_pi.Service.TransactionService;
import org.example.projet_pi.entity.Account;
import org.example.projet_pi.entity.Transaction;
import org.example.projet_pi.Dto.TransactionDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;

import org.example.projet_pi.Dto.AccountStatisticsDTO;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionService transactionService;

    // ==============================
    // 🔹 GET ALL TRANSACTIONS (ADMIN)
    // ==============================
    @GetMapping("/alltransaction")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // ==============================
    // 🔹 GET TRANSACTION BY ID (ADMIN, AGENT_FINANCE)
    // ==============================
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT_FINANCE')")
    public Transaction getTransactionById(@PathVariable Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id " + id));
    }

    // ==============================
    // 🔹 GET TRANSACTIONS BY ACCOUNT
    // ==============================
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT_FINANCE','AGENT_ASSURANCE','CLIENT')")
    public List<Transaction> getTransactionsByAccount(@PathVariable Long accountId) {
        return transactionRepository.findByAccountAccountId(accountId);
    }

    // ==============================
    // 🔹 CREATE TRANSACTION
    // ==============================
    @PostMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT_FINANCE')")
    public Transaction createTransaction(@PathVariable Long accountId,
                                         @RequestBody Transaction transaction) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        transaction.setAccount(account);
        transaction.setDate(LocalDate.now());

        // Mise à jour automatique du solde
        if (transaction.getType().equalsIgnoreCase("DEPOSIT")) {
            account.setBalance(account.getBalance() + transaction.getAmount());
        } else if (transaction.getType().equalsIgnoreCase("WITHDRAW")) {
            if (account.getBalance() < transaction.getAmount()) {
                throw new RuntimeException("Solde insuffisant !");
            }
            account.setBalance(account.getBalance() - transaction.getAmount());
        }

        accountRepository.save(account);
        return transactionRepository.save(transaction);
    }

    // ==============================
    // 🔹 UPDATE TRANSACTION (ADMIN)
    // ==============================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Transaction updateTransaction(@PathVariable Long id,
                                         @RequestBody Transaction transactionDetails) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id " + id));

        Account account = transaction.getAccount();

        // Retirer ancien montant
        if (transaction.getType().equalsIgnoreCase("DEPOSIT")) {
            account.setBalance(account.getBalance() - transaction.getAmount());
        } else if (transaction.getType().equalsIgnoreCase("WITHDRAW")) {
            account.setBalance(account.getBalance() + transaction.getAmount());
        }

        // Mise à jour transaction
        transaction.setAmount(transactionDetails.getAmount());
        transaction.setType(transactionDetails.getType());
        transaction.setDate(LocalDate.now());

        // Ajouter nouveau montant
        if (transaction.getType().equalsIgnoreCase("DEPOSIT")) {
            account.setBalance(account.getBalance() + transaction.getAmount());
        } else if (transaction.getType().equalsIgnoreCase("WITHDRAW")) {
            if (account.getBalance() < transaction.getAmount()) {
                throw new RuntimeException("Solde insuffisant !");
            }
            account.setBalance(account.getBalance() - transaction.getAmount());
        }

        accountRepository.save(account);
        return transactionRepository.save(transaction);
    }

    // ==============================
    // 🔹 DELETE TRANSACTION (ADMIN)
    // ==============================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteTransaction(@PathVariable Long id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id " + id));

        Account account = transaction.getAccount();

        // Ajuster le solde avant suppression
        if (transaction.getType().equalsIgnoreCase("DEPOSIT")) {
            account.setBalance(account.getBalance() - transaction.getAmount());
        } else if (transaction.getType().equalsIgnoreCase("WITHDRAW")) {
            account.setBalance(account.getBalance() + transaction.getAmount());
        }

        accountRepository.save(account);
        transactionRepository.delete(transaction);

        return "Transaction deleted successfully!";
    }

    // ==============================
    // 🔹 TRANSFER (ADMIN, AGENT_FINANCE)
    // ==============================
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT_FINANCE')")
    public String transfer(@RequestParam Long fromAccountId,
                           @RequestParam Long toAccountId,
                           @RequestParam double amount) {

        return transactionService.transferBetweenAccounts(
                fromAccountId, toAccountId, amount);
    }

    // ==============================
    // 🔹 HISTORY PAGINATED
    // ==============================
    @GetMapping("/history/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT_FINANCE','AGENT_ASSURANCE','CLIENT')")
    public Page<TransactionDTO> getHistory(
            @PathVariable Long accountId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {

        return transactionService.getTransactions(
                accountId, type, startDate, endDate, page, size);
    }

    // ==============================
    // 🔹 GENERATE PDF STATEMENT
    // ==============================
    @GetMapping("/statement/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT_FINANCE','CLIENT')")
    public ResponseEntity<byte[]> getStatement(@PathVariable Long accountId) throws Exception {

        byte[] pdf = transactionService.generateAccountStatement(accountId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData(
                "attachment", "extrait_compte_" + accountId + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }


    // 🆕 Statistiques du compte
    @GetMapping("/statistics/{accountId}")
    public ResponseEntity<AccountStatisticsDTO> getStatistics(
            @PathVariable Long accountId) {

        AccountStatisticsDTO stats = transactionService.getAccountStatistics(accountId);
        return ResponseEntity.ok(stats);
    }
}