package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Credit;
import org.example.projet_pi.entity.CreditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditRepository extends JpaRepository<Credit, Long> {

    // ✅ Vos méthodes existantes
    List<Credit> findByClientAndStatus(Client client, CreditStatus status);
    List<Credit> findByClient_Email(String email);

    // 🔴 NOUVELLES MÉTHODES POUR LES NOTIFICATIONS EMAIL
    List<Credit> findByDueDateAndStatusIn(LocalDate dueDate, List<CreditStatus> statuses);
    List<Credit> findByDueDateBeforeAndStatusIn(LocalDate date, List<CreditStatus> statuses);

    @Query("SELECT c FROM Credit c WHERE c.dueDate = :dueDate AND c.status IN :statuses")
    List<Credit> findCreditsByDueDateAndStatus(
            @Param("dueDate") LocalDate dueDate,
            @Param("statuses") List<CreditStatus> statuses
    );

    List<Credit> findByClientAndDueDateAfterAndStatusIn(
            Client client,
            LocalDate date,
            List<CreditStatus> statuses
    );

    List<Credit> findByStatusIn(List<CreditStatus> statuses);

    // ✅ AJOUT : Récupérer tous les crédits avec leurs clients
    @Query("SELECT c FROM Credit c LEFT JOIN FETCH c.client")
    List<Credit> findAllWithClient();

    // ✅ AJOUT : Récupérer un crédit spécifique avec son client
    @Query("SELECT c FROM Credit c LEFT JOIN FETCH c.client WHERE c.creditId = :creditId")
    Optional<Credit> findByIdWithClient(@Param("creditId") Long creditId);
    
    // ✅ AJOUT : Récupérer tous les crédits d'un client par son ID
    @Query("SELECT c FROM Credit c WHERE c.client.id = :clientId")
    List<Credit> findByClientId(@Param("clientId") Long clientId);
    
    // ✅ AJOUT : Récupérer les crédits d'un client par ID et statut
    @Query("SELECT c FROM Credit c WHERE c.client.id = :clientId AND c.status = :status")
    List<Credit> findByClientIdAndStatus(@Param("clientId") Long clientId, @Param("status") CreditStatus status);
}