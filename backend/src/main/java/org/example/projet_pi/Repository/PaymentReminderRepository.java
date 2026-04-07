package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Payment;
import org.example.projet_pi.entity.PaymentReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface PaymentReminderRepository extends JpaRepository<PaymentReminder, Long> {

    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN true ELSE false END FROM PaymentReminder pr " +
            "WHERE pr.payment = :payment AND pr.daysBefore = :daysBefore " +
            "AND pr.sentDate BETWEEN :startDate AND :endDate")
    boolean existsByPaymentAndDaysBeforeAndSentDateBetween(
            @Param("payment") Payment payment,
            @Param("daysBefore") int daysBefore,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
}