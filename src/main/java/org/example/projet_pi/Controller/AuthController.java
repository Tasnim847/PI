package org.example.projet_pi.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.UserRepository;
import org.example.projet_pi.Service.EmailService2;
import org.example.projet_pi.Service.SmsService;
import org.example.projet_pi.config.JwtUtils;
import org.example.projet_pi.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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
    private final EmailService2 emailService;
    private final SmsService smsService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user) {

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

            // ⭐ Envoi email bienvenue
            emailService.sendWelcomeEmail(
                    savedUser.getEmail(),
                    savedUser.getFirstName()

            );
            smsService.sendSms(
                    savedUser.getTelephone(),
                    savedUser.getFirstName()
            );

        }

        return ResponseEntity.ok("User registered successfully");
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User userRequest) {

        try {

            User user = userRepository.findByEmail(userRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 🚨 Vérifier si compte bloqué
            if(!user.isAccountNonLocked()){
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Account is blocked. Contact admin.");
            }

            // Authentification
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userRequest.getEmail(),
                            userRequest.getPassword()
                    )
            );

            // ✅ Reset attempts si login réussi
            user.setLoginAttempts(0);
            userRepository.save(user);

            // Token
            String token = jwtUtils.generateToken(
                    user.getEmail(),
                    user.getRole().name()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", user.getRole().name());

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e){

            User user = userRepository.findByEmail(userRequest.getEmail()).orElse(null);

            if(user != null){

                int attempts = user.getLoginAttempts() + 1;
                user.setLoginAttempts(attempts);

                // 🚨 Bloquer après 5 tentatives
                if(attempts >= 5){
                    user.setAccountNonLocked(false);
                }

                userRepository.save(user);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid Password");
        }
    }
    @PostMapping("/unlock-user/{id}")
    public ResponseEntity<?> unlockUser(@PathVariable Long id){

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setAccountNonLocked(true);
        user.setLoginAttempts(0);

        userRepository.save(user);

        return ResponseEntity.ok("User unlocked");
    }
}