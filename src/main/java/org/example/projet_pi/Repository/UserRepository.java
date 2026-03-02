package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    //  Recherche par firstName
    List<User> findByFirstNameContainingIgnoreCase(String firstName);

    //  Recherche par lastName
    List<User> findByLastNameContainingIgnoreCase(String lastName);

    // Recherche globale (recommandé pour admin)
    @Query("""
        SELECT u FROM User u 
        WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<User> searchUsers(@Param("keyword") String keyword);
}
