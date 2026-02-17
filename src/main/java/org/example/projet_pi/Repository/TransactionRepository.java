package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    //crud
    // 🔹 Trouver toutes les transactions d’un compte spécifique
    List<Transaction> findByAccountAccountId(Long accountId);

    // 🔹 Optionnel : transactions par type (DEPOSIT / WITHDRAW)
    List<Transaction> findByType(String type);

    // 🔹 Optionnel : transactions supérieures à un certain montant
    List<Transaction> findByAmountGreaterThan(double amount);
}
