package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    //  Trouver toutes les transactions d'un compte spécifique
    List<Transaction> findByAccountAccountId(Long accountId);

    //  transactions par type (DEPOSIT / WITHDRAW)
    List<Transaction> findByType(String type);

    // sstransactions supérieures à un certain montant
    List<Transaction> findByAmountGreaterThan(double amount);

    // ✅ NOUVEAU : Transactions d'un compte entre deux dates
    List<Transaction> findByAccountAccountIdAndDateBetween(
            Long accountId,
            LocalDate startDate,
            LocalDate endDate
    );



    // ✅ Pagination simple par compte
    Page<Transaction> findByAccountAccountId(
            Long accountId,
            Pageable pageable
    );

    // ✅ Pagination + filtre par type
    Page<Transaction> findByAccountAccountIdAndType(
            Long accountId,
            String type,
            Pageable pageable
    );

    // ✅ Pagination + filtre par date
    Page<Transaction> findByAccountAccountIdAndDateBetween(
            Long accountId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );


    // ✅ Pagination + type (JPQL)
    @Query("SELECT t FROM Transaction t WHERE t.account.accountId = :accountId " +
            "AND LOWER(t.type) = LOWER(:type)")
    Page<Transaction> findByAccountIdAndType(
            @Param("accountId") Long accountId,
            @Param("type") String type,
            Pageable pageable);

    // ✅ Pagination + type + date (JPQL)
    @Query("SELECT t FROM Transaction t WHERE t.account.accountId = :accountId " +
            "AND LOWER(t.type) = LOWER(:type) " +
            "AND t.date BETWEEN :start AND :end")
    Page<Transaction> findByAccountIdAndTypeAndDateBetween(
            @Param("accountId") Long accountId,
            @Param("type") String type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            Pageable pageable);



    // 🆕 Total des dépôts
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account.accountId = :accountId " +
            "AND LOWER(t.type) = 'deposit'")
    Double getTotalDeposits(@Param("accountId") Long accountId);

    // 🆕 Total des retraits
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account.accountId = :accountId " +
            "AND LOWER(t.type) = 'withdraw'")
    Double getTotalWithdrawals(@Param("accountId") Long accountId);

    // 🆕 Moyenne des transactions
    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.account.accountId = :accountId")
    Double getAverageTransactionAmount(@Param("accountId") Long accountId);

    // 🆕 Nombre total de transactions
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.accountId = :accountId")
    Long getTotalTransactions(@Param("accountId") Long accountId);

    // 🆕 Nombre de dépôts
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.accountId = :accountId " +
            "AND LOWER(t.type) = 'deposit'")
    Long getTotalDepositCount(@Param("accountId") Long accountId);

    // 🆕 Nombre de retraits
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.accountId = :accountId " +
            "AND LOWER(t.type) = 'withdraw'")
    Long getTotalWithdrawalCount(@Param("accountId") Long accountId);


}