package org.example.projet_pi.Controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.UserRepository;
import org.example.projet_pi.config.JwtUtils;
import org.example.projet_pi.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @GetMapping("/google/callback")
    public void googleCallback(
            @AuthenticationPrincipal OAuth2User principal,
            HttpServletResponse response) throws IOException {

        System.out.println("=== GOOGLE CALLBACK RECEIVED ===");

        if (principal == null) {
            System.out.println("Principal is null - redirecting to login");
            response.sendRedirect("http://localhost:4200/login?error=true");
            return;
        }

        String email = principal.getAttribute("email");
        System.out.println("Email from principal: " + email);

        if (email == null) {
            response.sendRedirect("http://localhost:4200/login?error=true");
            return;
        }

        // L'utilisateur devrait déjà exister grâce à CustomOAuth2UserService
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtils.generateToken(user);
        System.out.println("Token generated successfully");

        // Rediriger vers Angular avec le token
        String redirectUrl = "http://localhost:4200/oauth2/callback?token=" + token;
        response.sendRedirect(redirectUrl);
    }
}