package org.example.projet_pi.Controller;

import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.Repository.TransactionRepository;
import org.example.projet_pi.entity.Account;
import org.example.projet_pi.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    // 🔹 Get all transactions
    @GetMapping
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }


    @GetMapping("/{id}")
    public Transaction getTransactionById(@PathVariable Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id " + id));
    }


    @GetMapping("/account/{accountId}")
    public List<Transaction> getTransactionsByAccount(@PathVariable Long accountId) {
        return transactionRepository.findByAccountAccountId(accountId);
    }

    @PostMapping("/account/{accountId}")
    public Transaction createTransaction(@PathVariable Long accountId,
                                         @RequestBody Transaction transaction) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        transaction.setAccount(account);
        transaction.setDate(LocalDate.now());

        // 💰 Mise à jour automatique du solde
        if (transaction.getType().equalsIgnoreCase("DEPOSIT")) {
            account.setBalance(account.getBalance() + transaction.getAmount());
        } else if (transaction.getType().equalsIgnoreCase("WITHDRAW")) {
            account.setBalance(account.getBalance() - transaction.getAmount());
        }

        accountRepository.save(account);
        return transactionRepository.save(transaction);
    }


    //crud
    @PutMapping("/{id}")
    public Transaction updateTransaction(@PathVariable Long id, @RequestBody Transaction transactionDetails) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id " + id));

        Account account = transaction.getAccount();

        // 💰 Ajustement du solde : retirer l'ancien montant
        if (transaction.getType().equalsIgnoreCase("DEPOSIT")) {
            account.setBalance(account.getBalance() - transaction.getAmount());
        } else if (transaction.getType().equalsIgnoreCase("WITHDRAW")) {
            account.setBalance(account.getBalance() + transaction.getAmount());
        }

        // Mettre à jour la transaction
        transaction.setAmount(transactionDetails.getAmount());
        transaction.setType(transactionDetails.getType());
        transaction.setDate(LocalDate.now());

        // 💰 Ajouter le nouveau montant
        if (transaction.getType().equalsIgnoreCase("DEPOSIT")) {
            account.setBalance(account.getBalance() + transaction.getAmount());
        } else if (transaction.getType().equalsIgnoreCase("WITHDRAW")) {
            account.setBalance(account.getBalance() - transaction.getAmount());
        }

        accountRepository.save(account);
        return transactionRepository.save(transaction);
    }


    @DeleteMapping("/{id}")
    public String deleteTransaction(@PathVariable Long id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id " + id));

        Account account = transaction.getAccount();

        // 💰 Ajuster le solde avant suppression
        if (transaction.getType().equalsIgnoreCase("DEPOSIT")) {
            account.setBalance(account.getBalance() - transaction.getAmount());
        } else if (transaction.getType().equalsIgnoreCase("WITHDRAW")) {
            account.setBalance(account.getBalance() + transaction.getAmount());
        }

        accountRepository.save(account);
        transactionRepository.delete(transaction);

        return "Transaction deleted successfully!";
    }
}
