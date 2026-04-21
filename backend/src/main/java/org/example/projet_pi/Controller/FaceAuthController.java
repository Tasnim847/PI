package org.example.projet_pi.Controller;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.FaceDataRepository;
import org.example.projet_pi.Repository.UserRepository;
import org.example.projet_pi.Service.FaceRecognitionService;
import org.example.projet_pi.config.JwtUtils;
import org.example.projet_pi.entity.FaceData;
import org.example.projet_pi.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/face")
@RequiredArgsConstructor
public class FaceAuthController {

    private final FaceRecognitionService faceRecognitionService;
    private final FaceDataRepository faceDataRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    /**
     * Enregistrement du visage d'un utilisateur
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerFace(
            Authentication authentication,
            @RequestParam("faceImage") MultipartFile faceImage
    ) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Vérifier si l'utilisateur a déjà enregistré son visage
            if (faceDataRepository.findByUser(user).isPresent()) {
                return ResponseEntity.badRequest()
                        .body("Vous avez déjà enregistré votre visage. Utilisez /update pour le mettre à jour.");
            }

            // Extraire les features du visage
            byte[] faceFeatures = faceRecognitionService.extractFaceFeatures(faceImage);

            // Sauvegarder
            FaceData faceData = new FaceData(user, faceFeatures);
            faceDataRepository.save(faceData);

            return ResponseEntity.ok(Map.of(
                    "message", "Visage enregistré avec succès",
                    "registeredAt", faceData.getRegisteredAt()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erreur lors de l'enregistrement: " + e.getMessage());
        }
    }

    /**
     * Login par reconnaissance faciale
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginWithFace(
            @RequestParam("faceImage") MultipartFile faceImage
    ) {
        try {
            // Extraire les features du visage reçu
            byte[] uploadedFaceFeatures = faceRecognitionService.extractFaceFeatures(faceImage);

            // Récupérer tous les visages enregistrés
            List<FaceData> allFaces = faceDataRepository.findAll();

            User matchedUser = null;
            double bestMatchScore = 0;
            double threshold = 0.65; // Seuil de similarité (ajustable)

            for (FaceData faceData : allFaces) {
                double similarity = faceRecognitionService.compareFaces(
                        uploadedFaceFeatures,
                        faceData.getFaceFeatures()
                );

                System.out.println("Similarité avec " + faceData.getUser().getEmail() + ": " + similarity);

                if (similarity > bestMatchScore && similarity > threshold) {
                    bestMatchScore = similarity;
                    matchedUser = faceData.getUser();
                }
            }

            if (matchedUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Visage non reconnu. Veuillez réessayer ou utiliser le login classique.");
            }

            // Vérifier si le compte est bloqué
            if (!matchedUser.isAccountNonLocked()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Votre compte est bloqué. Utilisez le login classique pour le débloquer.");
            }

            // Mettre à jour la dernière vérification
            FaceData faceData = faceDataRepository.findByUser(matchedUser).get();
            faceData.setLastVerifiedAt(LocalDateTime.now());
            faceDataRepository.save(faceData);

            // Générer le token JWT
            String token = jwtUtils.generateToken(matchedUser);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", matchedUser.getRole().name());
            response.put("userId", matchedUser.getId());
            response.put("firstName", matchedUser.getFirstName());
            response.put("lastName", matchedUser.getLastName());
            response.put("similarityScore", bestMatchScore);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erreur lors de la reconnaissance: " + e.getMessage());
        }
    }

    /**
     * Mise à jour du visage
     */
    @PutMapping("/update")
    public ResponseEntity<?> updateFace(
            Authentication authentication,
            @RequestParam("faceImage") MultipartFile faceImage
    ) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            FaceData faceData = faceDataRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Aucun visage enregistré pour cet utilisateur"));

            byte[] newFaceFeatures = faceRecognitionService.extractFaceFeatures(faceImage);
            faceData.setFaceFeatures(newFaceFeatures);
            faceDataRepository.save(faceData);

            return ResponseEntity.ok("Visage mis à jour avec succès");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    /**
     * Vérifier si l'utilisateur a un visage enregistré
     */
    @GetMapping("/status")
    public ResponseEntity<?> getFaceStatus(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        boolean hasFace = faceDataRepository.findByUser(user).isPresent();

        return ResponseEntity.ok(Map.of(
                "hasFaceRegistered", hasFace,
                "email", email
        ));
    }

    /**
     * Supprimer le visage enregistré
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFace(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        faceDataRepository.findByUser(user).ifPresent(faceDataRepository::delete);

        return ResponseEntity.ok("Visage supprimé avec succès");
    }
    // Ajoutez cette méthode dans FaceAuthController.java

    @GetMapping("/check/{email}")
    public ResponseEntity<?> checkFaceExistsByEmail(@PathVariable String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.ok(Map.of("hasFaceRegistered", false));
            }

            boolean hasFace = faceDataRepository.findByUser(user).isPresent();

            return ResponseEntity.ok(Map.of(
                    "hasFaceRegistered", hasFace,
                    "email", email
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("hasFaceRegistered", false));
        }
    }
}