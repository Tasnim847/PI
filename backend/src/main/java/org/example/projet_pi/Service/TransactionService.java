package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.AccountStatisticsDTO;
import org.example.projet_pi.Dto.TransactionDTO;
import org.example.projet_pi.Repository.AccountRepository;
import org.example.projet_pi.Repository.TransactionRepository;
import org.example.projet_pi.entity.Account;
import org.example.projet_pi.entity.Transaction;
import org.example.projet_pi.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
//crud
@Service
public class TransactionService implements ITransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PdfService pdfService;
    private final EmailActionService emailActionService;

    // Injection des repositories via le constructeur
    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              PdfService pdfService,
                              EmailActionService emailActionService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.pdfService = pdfService;
        this.emailActionService = emailActionService;
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

        // 🆕 Envoyer email de confirmation
        String clientEmail = account.getClient() != null
                ? account.getClient().getEmail()
                : null;

        if (clientEmail != null) {
            emailActionService.sendTransactionEmail(
                    clientEmail,
                    transaction.getType(),
                    transaction.getAmount(),
                    account.getBalance()
            );
        }
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


    // 🆕 MÉTIER AVANCÉ : Historique filtrable et paginé avec DTO
    public Page<TransactionDTO> getTransactions(
            Long accountId,
            String type,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

        Page<Transaction> transactions;

        if (type != null && startDate != null && endDate != null) {
            // ✅ Correct
            transactions = transactionRepository
                    .findByAccountIdAndTypeAndDateBetween(
                            accountId, type, startDate, endDate, pageable);
        }
        else if (type != null) {
            transactions = transactionRepository
                    .findByAccountAccountIdAndType(accountId, type, pageable);
        }
        else if (startDate != null && endDate != null) {
            transactions = transactionRepository
                    .findByAccountAccountIdAndDateBetween(accountId, startDate, endDate, pageable);
        }
        else {
            transactions = transactionRepository
                    .findByAccountAccountId(accountId, pageable);
        }

        return transactions.map(this::convertToDTO);
    }

    // 🔄 Mapper Entity → DTO
    private TransactionDTO convertToDTO(Transaction t) {
        return new TransactionDTO(
                t.getTransactionId(),
                t.getAmount(),
                t.getType(),
                t.getDate()
        );
    }

    // 🆕 Générer PDF extrait de compte
    public byte[] generateAccountStatement(Long accountId) throws Exception {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        List<Transaction> transactions = transactionRepository
                .findByAccountAccountId(accountId);

        return pdfService.generateStatement(account, transactions);
    }



    // 🆕 Statistiques du compte
    public AccountStatisticsDTO getAccountStatistics(Long accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        double totalDeposits = transactionRepository.getTotalDeposits(accountId) != null
                ? transactionRepository.getTotalDeposits(accountId) : 0.0;

        double totalWithdrawals = transactionRepository.getTotalWithdrawals(accountId) != null
                ? transactionRepository.getTotalWithdrawals(accountId) : 0.0;

        double averageAmount = transactionRepository.getAverageTransactionAmount(accountId) != null
                ? transactionRepository.getAverageTransactionAmount(accountId) : 0.0;

        long totalTransactions = transactionRepository.getTotalTransactions(accountId) != null
                ? transactionRepository.getTotalTransactions(accountId) : 0L;

        long totalDepositCount = transactionRepository.getTotalDepositCount(accountId) != null
                ? transactionRepository.getTotalDepositCount(accountId) : 0L;

        long totalWithdrawalCount = transactionRepository.getTotalWithdrawalCount(accountId) != null
                ? transactionRepository.getTotalWithdrawalCount(accountId) : 0L;

        return new AccountStatisticsDTO(
                accountId,
                totalDeposits,
                totalWithdrawals,
                account.getBalance(),
                averageAmount,
                totalTransactions,
                totalDepositCount,
                totalWithdrawalCount
        );
    }

    // ============================================================
// 🆕 TRANSFERT PAR RIP
// ============================================================

    /**
     * Transfert d'argent entre comptes par RIP
     */
    @Transactional
    public String transferByRip(String sourceRip, String targetRip, double amount, String description, Long clientId) {

        // 1. Validation du montant
        if (amount <= 0) {
            throw new RuntimeException("Le montant doit être positif");
        }

        // 2. Récupérer les comptes
        Account sourceAccount = accountRepository.findByRip(sourceRip)
                .orElseThrow(() -> new RuntimeException("Compte source non trouvé"));

        Account targetAccount = accountRepository.findByRip(targetRip)
                .orElseThrow(() -> new RuntimeException("Compte destination non trouvé"));

        // 3. Vérifier que le compte source appartient bien au client
        if (!sourceAccount.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Vous n'êtes pas propriétaire de ce compte");
        }

        // 4. Vérifier que les comptes sont actifs
        if (!"ACTIVE".equals(sourceAccount.getStatus())) {
            throw new RuntimeException("Votre compte n'est pas actif");
        }

        if (!"ACTIVE".equals(targetAccount.getStatus())) {
            throw new RuntimeException("Le compte destinataire n'est pas actif");
        }

        // 5. Vérifier le solde
        if (sourceAccount.getBalance() < amount) {
            throw new RuntimeException("Solde insuffisant. Solde actuel: " + sourceAccount.getBalance());
        }

        // 6. Vérifier les limites de virement quotidiennes
        checkDailyTransferLimit(sourceAccount, amount);

        // 7. Effectuer le virement
        sourceAccount.setBalance(sourceAccount.getBalance() - amount);
        targetAccount.setBalance(targetAccount.getBalance() + amount);

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        // 8. Enregistrer les transactions
        Transaction debitTransaction = new Transaction();
        debitTransaction.setAccount(sourceAccount);
        debitTransaction.setAmount(amount);
        debitTransaction.setType(TransactionType.WITHDRAW.name());
        debitTransaction.setDate(java.time.LocalDate.now());
        debitTransaction.setDescription("VIREMENT SORTANT vers " + targetRip + " - " + description);
        transactionRepository.save(debitTransaction);

        Transaction creditTransaction = new Transaction();
        creditTransaction.setAccount(targetAccount);
        creditTransaction.setAmount(amount);
        creditTransaction.setType(TransactionType.DEPOSIT.name());
        creditTransaction.setDate(java.time.LocalDate.now());
        creditTransaction.setDescription("VIREMENT ENTRANT de " + sourceRip + " - " + description);
        transactionRepository.save(creditTransaction);

        return "Virement de " + amount + " TND effectué avec succès vers le compte " + targetRip;
    }

    /**
     * Vérifie la limite quotidienne de virement
     */
    private void checkDailyTransferLimit(Account account, double amount) {
        java.time.LocalDate today = java.time.LocalDate.now();

        double todayTransfers = transactionRepository
                .findByAccountAccountIdAndDate(account.getAccountId(), today)
                .stream()
                .filter(t -> t.getType().equals("WITHDRAW"))
                .filter(t -> t.getDescription() != null && t.getDescription().startsWith("VIREMENT SORTANT"))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double limit = account.getDailyTransferLimit();
        if (limit > 0 && (todayTransfers + amount) > limit) {
            throw new RuntimeException("Limite quotidienne de virement dépassée. " +
                    "Limite: " + limit + " TND, " +
                    "Déjà viré aujourd'hui: " + todayTransfers + " TND, " +
                    "Montant demandé: " + amount + " TND");
        }
    }


    public List<Transaction> getTransactionsByAccountAndDate(Long accountId, java.time.LocalDate date) {
        return transactionRepository.findByAccountAccountIdAndDate(accountId, date);
    }



}
