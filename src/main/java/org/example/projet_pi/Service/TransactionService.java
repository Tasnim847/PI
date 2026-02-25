package org.example.projet_pi.Service;
import org.springframework.transaction.annotation.Transactional;


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
            // ✅ NOUVEAU : Vérifier les limites avant retrait
            checkWithdrawLimits(account, transaction.getAmount());
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
    // 🔹 Advanced: Transfer between accounts
    @Transactional
    public String transferBetweenAccounts(Long fromAccountId, Long toAccountId, double amount) {

        if (fromAccountId.equals(toAccountId)) {
            throw new RuntimeException("Cannot transfer to the same account");
        }

        if (amount <= 0) {
            throw new RuntimeException("Amount must be positive");
        }

        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new RuntimeException("Destination account not found"));

        if (fromAccount.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }

        // 🔻 Debit source account
        fromAccount.setBalance(fromAccount.getBalance() - amount);
        accountRepository.save(fromAccount);

        // 🔺 Credit destination account
        toAccount.setBalance(toAccount.getBalance() + amount);
        accountRepository.save(toAccount);

        // 🧾 Create debit transaction
        Transaction debit = new Transaction();
        debit.setAccount(fromAccount);
        debit.setAmount(amount);
        debit.setType(TransactionType.WITHDRAW.name());
        debit.setDate(LocalDate.now());
        transactionRepository.save(debit);

        // 🧾 Create credit transaction
        Transaction credit = new Transaction();
        credit.setAccount(toAccount);
        credit.setAmount(amount);
        credit.setType(TransactionType.DEPOSIT.name());
        credit.setDate(LocalDate.now());
        transactionRepository.save(credit);

        return "Transfer successful";
    }
    // 🔹 MÉTIER AVANCÉ : Calculer total retraits du jour
    private double getDailyWithdrawTotal(Long accountId) {
        LocalDate today = LocalDate.now();
        List<Transaction> transactions = transactionRepository
                .findByAccountAccountIdAndDateBetween(accountId, today, today);

        return transactions.stream()
                .filter(t -> t.getType().equalsIgnoreCase("WITHDRAW"))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    // 🔹 MÉTIER AVANCÉ : Calculer total retraits du mois
    private double getMonthlyWithdrawTotal(Long accountId) {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();
        List<Transaction> transactions = transactionRepository
                .findByAccountAccountIdAndDateBetween(accountId, startOfMonth, today);

        return transactions.stream()
                .filter(t -> t.getType().equalsIgnoreCase("WITHDRAW"))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    // 🔹 MÉTIER AVANCÉ : Vérifier les limites avant retrait
    public void checkWithdrawLimits(Account account, double amount) {

        // Vérifier limite quotidienne
        if (account.getDailyLimit() > 0) {
            double dailyTotal = getDailyWithdrawTotal(account.getAccountId());
            if (dailyTotal + amount > account.getDailyLimit()) {
                throw new RuntimeException(
                        "Daily withdrawal limit exceeded. " +
                                "Daily limit: " + account.getDailyLimit() +
                                ", Already withdrawn today: " + dailyTotal +
                                ", Requested: " + amount
                );
            }
        }

        // Vérifier limite mensuelle
        if (account.getMonthlyLimit() > 0) {
            double monthlyTotal = getMonthlyWithdrawTotal(account.getAccountId());
            if (monthlyTotal + amount > account.getMonthlyLimit()) {
                throw new RuntimeException(
                        "Monthly withdrawal limit exceeded. " +
                                "Monthly limit: " + account.getMonthlyLimit() +
                                ", Already withdrawn this month: " + monthlyTotal +
                                ", Requested: " + amount
                );
            }
        }
    }
}
