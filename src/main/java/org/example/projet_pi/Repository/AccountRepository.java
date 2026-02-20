package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {


    //  Récupérer les comptes d'un client spécifique
    List<Account> findByClientId(Long clientId);

    //  Récupérer les comptes selon leur statut
    List<Account> findByStatus(String status);
}
