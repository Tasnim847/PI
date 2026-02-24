package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // 🔹 Recherche par statut
    List<Complaint> findByStatus(String status);

    // 🔹 Recherche par client (id hérité de User)
    List<Complaint> findByClient_Id(Long clientId);

    // 🔹 Recherche par agent assurance
    List<Complaint> findByAgentAssurance_Id(Long agentAssuranceId);

    // 🔹 Recherche par agent finance
    List<Complaint> findByAgentFinance_Id(Long agentFinanceId);

    // 🔹 Recherche avancée dynamique
    @Query("""
        SELECT c FROM Complaint c
        WHERE (:status IS NULL OR c.status = :status)
        AND (:keyword IS NULL OR LOWER(c.message) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:clientId IS NULL OR c.client.id = :clientId)
        AND (:agentAssuranceId IS NULL OR c.agentAssurance.id = :agentAssuranceId)
        AND (:agentFinanceId IS NULL OR c.agentFinance.id = :agentFinanceId)
        AND (:dateDebut IS NULL OR c.date >= :dateDebut)
        AND (:dateFin IS NULL OR c.date <= :dateFin)
    """)
    List<Complaint> searchComplaints(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("clientId") Long clientId,
            @Param("agentAssuranceId") Long agentAssuranceId,
            @Param("agentFinanceId") Long agentFinanceId,
            @Param("dateDebut") Date dateDebut,
            @Param("dateFin") Date dateFin
    );
}