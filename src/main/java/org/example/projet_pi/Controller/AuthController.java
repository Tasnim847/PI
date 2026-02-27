package org.example.projet_pi.Controller;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.UserRepository;
import org.example.projet_pi.config.JwtUtils;
import org.example.projet_pi.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        // Vérifier si l'email existe déjà
        if(userRepository.findByEmail(user.getEmail()).isPresent()){
            return ResponseEntity.badRequest().body("Email already exists");
        }

        // Encoder le mot de passe
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Si aucun rôle n'est fourni, on suppose que c'est un client
        if(user.getRole() == null) {
            user.setRole(Role.CLIENT);
        }

        // Vérification des rôles autorisés pour s'enregistrer
        if(user.getRole() != Role.CLIENT && user.getRole() != Role.ADMIN){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Seuls les clients ou l'admin peuvent créer des comptes");
        }

        User savedUser;

        switch (user.getRole()) {
            case ADMIN:
                Admin admin = new Admin();
                admin.setFirstName(user.getFirstName());
                admin.setLastName(user.getLastName());
                admin.setEmail(user.getEmail());
                admin.setPassword(user.getPassword());
                admin.setTelephone(user.getTelephone());
                admin.setRole(Role.ADMIN);
                savedUser = userRepository.save(admin);
                break;
            default: // CLIENT
                Client client = new Client();
                client.setFirstName(user.getFirstName());
                client.setLastName(user.getLastName());
                client.setEmail(user.getEmail());
                client.setPassword(user.getPassword());
                client.setTelephone(user.getTelephone());
                client.setRole(Role.CLIENT);
                savedUser = userRepository.save(client);
                break;
        }

        return ResponseEntity.ok(savedUser);
    }


/*
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User userRequest) {
        try {
            // Authentification via Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userRequest.getEmail(),
                            userRequest.getPassword()
                    )
            );

            // Récupérer l'utilisateur complet depuis la base
            User user = userRepository.findByEmail(userRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Générer le token JWT
            String token = jwtUtils.generateToken(user.getEmail());

            // Préparer la réponse
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("role", user.getRole());
            data.put("id", user.getId());
            data.put("firstName", user.getFirstName());
            data.put("lastName", user.getLastName());
            data.put("email", user.getEmail());

            // Ajouter des infos spécifiques selon le type
            if (user instanceof Client client) {
                data.put("agentAssuranceId", client.getAgentAssurance() != null ? client.getAgentAssurance().getId() : null);
                data.put("agentFinanceId", client.getAgentFinance() != null ? client.getAgentFinance().getId() : null);
            }

            if (user instanceof AgentAssurance agentAssurance) {
                data.put("clientsCount", agentAssurance.getClients() != null ? agentAssurance.getClients().size() : 0);
            }

            if (user instanceof AgentFinance agentFinance) {
                data.put("clientsCount", agentFinance.getClients() != null ? agentFinance.getClients().size() : 0);
            }

            return ResponseEntity.ok(data);

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid credentials");
        }
    }
*/

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User userRequest) {

        try {

            // 1️⃣ Authentification
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userRequest.getEmail(),
                            userRequest.getPassword()
                    )
            );

            // 2️⃣ Récupérer user complet
            User user = userRepository.findByEmail(userRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 3️⃣ Générer token avec role
            String token = jwtUtils.generateToken(
                    user.getEmail(),
                    user.getRole().name()
            );

            // 4️⃣ Construire réponse propre
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", user.getRole().name());
            response.put("id", user.getId());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("email", user.getEmail());

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid credentials");
        }
    }
}