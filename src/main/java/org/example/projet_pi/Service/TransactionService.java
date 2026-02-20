package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.Repository.TransactionRepository;
import org.example.projet_pi.entity.Account;
import org.example.projet_pi.entity.Transaction;
import org.example.projet_pi.entity.TransactionType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

//crud
@Service
public class TransactionService implements ITransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    // Injection des repositories via le constructeur
    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    // 🔹 Ajouter une transaction (dépôt ou retrait)
    @Override
    public Transaction addTransaction(Long accountId, Transaction transaction) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with id " + accountId));

        // Définir la date et lier le compte
        transaction.setDate(LocalDate.now());
        transaction.setAccount(account);

        // Mise à jour du solde selon le type de transaction
        if (transaction.getType().equalsIgnoreCase(TransactionType.DEPOSIT.name())) {
            account.setBalance(account.getBalance() + transaction.getAmount());
        } else if (transaction.getType().equalsIgnoreCase(TransactionType.WITHDRAW.name())) {
            if (account.getBalance() < transaction.getAmount()) {
                throw new RuntimeException("Insufficient balance for withdrawal");
            }
            account.setBalance(account.getBalance() - transaction.getAmount());
        } else {
            throw new RuntimeException("Invalid transaction type");
        }

        // Sauvegarder le compte mis à jour et la transaction
        accountRepository.save(account);
        return transactionRepository.save(transaction);
    }

    // 🔹 Supprimer une transaction
    @Override
    public void deleteTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id " + transactionId));
        transactionRepository.delete(transaction);
    }

    // 🔹 Récupérer une transaction par ID
    @Override
    public Transaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id " + transactionId));
    }

    // 🔹 Récupérer toutes les transactions
    @Override
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // 🔹 Récupérer toutes les transactions d’un compte spécifique
    @Override
    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByAccountAccountId(accountId);
    }
}
