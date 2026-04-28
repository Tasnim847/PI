package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.AccountRequest;
import org.example.projet_pi.entity.AccountRequestStatus;  // Changé
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {

    List<AccountRequest> findByClientId(Long clientId);

    List<AccountRequest> findByStatus(AccountRequestStatus status);  // Changé

    List<AccountRequest> findByStatusAndClientId(AccountRequestStatus status, Long clientId);  // Changé
}