package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

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
}