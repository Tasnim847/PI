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

        if(userRepository.findByEmail(user.getEmail()).isPresent()){
            return ResponseEntity.badRequest().body("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if(user.getRole() == null) {
            user.setRole(Role.CLIENT);
        }

        User savedUser;

        if(user.getRole() == Role.ADMIN){

            Admin admin = new Admin();
            admin.setFirstName(user.getFirstName());
            admin.setLastName(user.getLastName());
            admin.setEmail(user.getEmail());
            admin.setPassword(user.getPassword());
            admin.setTelephone(user.getTelephone());
            admin.setRole(Role.ADMIN);

            savedUser = userRepository.save(admin);

        } else {

            Client client = new Client();
            client.setFirstName(user.getFirstName());
            client.setLastName(user.getLastName());
            client.setEmail(user.getEmail());
            client.setPassword(user.getPassword());
            client.setTelephone(user.getTelephone());
            client.setRole(Role.CLIENT);

            savedUser = userRepository.save(client);
        }

        return ResponseEntity.ok("User registered successfully");
    }


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