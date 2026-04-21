package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    List<LoginHistory> findByUserId(Long userId);

    Page<LoginHistory> findAll(Pageable pageable);

    List<LoginHistory> findTop50ByOrderByLoginTimeDesc();

    @Query("SELECT l FROM LoginHistory l WHERE l.latitude IS NOT NULL " +
            "AND l.longitude IS NOT NULL ORDER BY l.loginTime DESC")
    List<LoginHistory> findAllWithLocation();

    @Query("SELECT l FROM LoginHistory l WHERE l.latitude IS NOT NULL " +
            "AND l.longitude IS NOT NULL " +
            "AND l.loginTime BETWEEN :startDate AND :endDate ORDER BY l.loginTime DESC")
    List<LoginHistory> findWithLocationBetween(@Param("startDate") Date startDate,
                                               @Param("endDate") Date endDate);

    // ✅ NOUVEAU: Dernière connexion par utilisateur distinct
    @Query(value = "SELECT * FROM login_history l1 " +
            "WHERE l1.login_time = ( " +
            "    SELECT MAX(l2.login_time) FROM login_history l2 " +
            "    WHERE l2.user_id = l1.user_id " +
            ") " +
            "AND l1.latitude IS NOT NULL " +
            "AND l1.longitude IS NOT NULL " +
            "ORDER BY l1.login_time DESC " +
            "LIMIT 100",
            nativeQuery = true)
    List<LoginHistory> findLastConnectionPerUser();

    @Query(value = "SELECT COUNT(*) FROM login_history l " +
            "WHERE DAYOFWEEK(l.login_time) BETWEEN 2 AND 6 " +
            "AND l.login_time BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    long countWeekdayConnectionsNative(@Param("startDate") Date startDate,
                                       @Param("endDate") Date endDate);

    @Query(value = "SELECT COUNT(*) FROM login_history l " +
            "WHERE (DAYOFWEEK(l.login_time) = 1 OR DAYOFWEEK(l.login_time) = 7) " +
            "AND l.login_time BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    long countWeekendConnectionsNative(@Param("startDate") Date startDate,
                                       @Param("endDate") Date endDate);

    @Query("SELECT COUNT(l) FROM LoginHistory l WHERE l.user.id = :userId " +
            "AND l.loginTime BETWEEN :startDate AND :endDate")
    long countByUserIdAndLoginTimeBetween(@Param("userId") Long userId,
                                          @Param("startDate") Date startDate,
                                          @Param("endDate") Date endDate);

    List<LoginHistory> findByLoginTimeBetween(Date startDate, Date endDate);

    @Query("SELECT l.country, COUNT(l) FROM LoginHistory l " +
            "WHERE l.country IS NOT NULL GROUP BY l.country ORDER BY COUNT(l) DESC")
    List<Object[]> countByCountry();

    @Query("SELECT l.city, l.country, COUNT(l) FROM LoginHistory l " +
            "WHERE l.city IS NOT NULL GROUP BY l.city, l.country ORDER BY COUNT(l) DESC")
    List<Object[]> countByCity();

}