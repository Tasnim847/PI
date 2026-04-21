package org.example.projet_pi.Repository;

import org.example.projet_pi.entity.FaceData;
import org.example.projet_pi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FaceDataRepository extends JpaRepository<FaceData, Long> {
    Optional<FaceData> findByUser(User user);
    Optional<FaceData> findByUserId(Long userId);
}