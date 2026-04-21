package org.example.projet_pi.config;

import org.example.projet_pi.Repository.UserRepository;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            String email = oAuth2User.getAttribute("email");
            String firstName = oAuth2User.getAttribute("given_name");
            String lastName = oAuth2User.getAttribute("family_name");
            String picture = oAuth2User.getAttribute("picture");

            System.out.println("=== CustomOAuth2UserService ===");
            System.out.println("Email: " + email);

            if (email != null && !userRepository.findByEmail(email).isPresent()) {
                Client newClient = new Client();
                newClient.setEmail(email);

                // ✅ Nettoyer les noms pour respecter la validation ^[a-zA-Z]+$
                newClient.setFirstName(firstName != null ? firstName.replaceAll("[^a-zA-Z]", "") : "Unknown");
                newClient.setLastName(lastName != null ? lastName.replaceAll("[^a-zA-Z]", "") : "Unknown");

                newClient.setTelephone(null);
                newClient.setOauthUser(true);
                newClient.setPhoto(picture);
                newClient.setRole(Role.CLIENT);
                newClient.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

                userRepository.save(newClient);
                System.out.println("New user created: " + email);
            } else {
                System.out.println("User already exists: " + email);
            }

        } catch (Exception e) {
            // This will show the REAL error in your logs
            System.err.println("=== ERROR in CustomOAuth2UserService ===");
            e.printStackTrace();
            throw new OAuth2AuthenticationException("Failed to process OAuth2 user: " + e.getMessage());
        }

        return oAuth2User;
    }
}