package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Credit;
import org.example.projet_pi.entity.CreditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditRepository extends JpaRepository<Credit, Long>{
    // Récupérer tous les crédits CLOSED d’un client
    List<Credit> findByClientAndStatus(Client client, CreditStatus status);
    List<Credit> findByClient_Email(String email);
}
