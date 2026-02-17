package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentRepository extends JpaRepository<Repayment, Long> {
}

