package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.Admin;
import org.example.projet_pi.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    @Query("SELECT a FROM Admin a WHERE a.email = ?1")
    Admin findAdminByEmail(String email);
}
