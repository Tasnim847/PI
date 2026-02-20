package org.example.projet_pi.Repository;


import org.example.projet_pi.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Optionnel : récupérer tous les documents d'un Claim
    List<Document> findByClaimClaimId(Long claimId);
}
