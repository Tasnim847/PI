package org.example.projet_pi.config;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Role;
import org.example.projet_pi.entity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.UUID;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity

public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final org.example.projet_pi.Repository.UserRepository userRepository;  // ADD THIS
    private final JwtUtils jwtUtils;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final PasswordEncoder passwordEncoder;



    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public MultipartFilter multipartFilter() {
        MultipartFilter multipartFilter = new MultipartFilter();
        multipartFilter.setMultipartResolverBeanName("multipartResolver");
        return multipartFilter;
    }

    // AJOUTEZ CETTE METHODE pour la configuration CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // AJOUTEZ CETTE LIGNE
                .csrf(csrf -> csrf.disable())

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(multipartFilter(), JwtAuthenticationFilter.class)


                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/face/login").permitAll()      // Login facial sans token
                        .requestMatchers("/api/auth/face/check/**").permitAll()  // Vérifier si visage existe
                        .requestMatchers("/api/auth/face/register").authenticated() // Enregistrement nécessite auth
                        .requestMatchers("/uploads/**").permitAll()
                        //  Endpoints publics
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/otp/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/products/images/**").permitAll()
                        .requestMatchers("/api/auth/me").authenticated()
                        // ✅ REMPLACER par des règles précises
                        .requestMatchers("/agents/finance/**").hasAnyRole("AGENT_FINANCE", "ADMIN")  // finance
                        .requestMatchers("/agents-assurance/**").hasAnyRole("AGENT_ASSURANCE", "ADMIN")  // assurance (déjà OK)
                        .requestMatchers("/api/clients/all").hasAnyRole("ADMIN", "AGENT_FINANCE")
                        .requestMatchers("/api/clients/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Dans authorizeHttpRequests — ajouter avec les routes publiques
                        .requestMatchers("/api/auth/google/**").permitAll()
                        .requestMatchers("/login/oauth2/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()


                        // 👑 ADMIN uniquement
                        .requestMatchers("/products/addProduct").hasRole("ADMIN")
                        .requestMatchers("/products/updateProduct").hasRole("ADMIN")
                        .requestMatchers("/products/deleteProduct/**").hasRole("ADMIN")
                        .requestMatchers("/products/activeProducts").hasAnyRole("CLIENT", "AGENT_ASSURANCE")
                        .requestMatchers("/admins/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/*/image").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/*/image").hasRole("ADMIN")

                        // 🎯 AGENT_ASSURANCE
                        .requestMatchers("/agents-assurance/**").hasAnyRole("AGENT_ASSURANCE", "ADMIN")

                        // 👤 CLIENT - Gestion des contrats
                        .requestMatchers("/contrats/addCont").hasRole("CLIENT")
                        .requestMatchers("/contrats/updateCont").hasRole("CLIENT")
                        .requestMatchers("/contrats/deleteCont/**").hasRole("CLIENT")
                        .requestMatchers("/contrats/getCont/**").hasAnyRole("CLIENT", "AGENT_ASSURANCE", "ADMIN")
                        .requestMatchers("/contrats/allCont").hasAnyRole("AGENT_ASSURANCE", "ADMIN")
                        .requestMatchers("/contrats/activate/**").hasRole("AGENT_ASSURANCE")
                        .requestMatchers("/contrats/{id}/download/pdf").hasAnyRole("CLIENT", "AGENT_ASSURANCE", "ADMIN")
                        .requestMatchers("/contrats/{id}/risk").hasAnyRole("CLIENT", "AGENT_ASSURANCE", "ADMIN")
                        .requestMatchers("/contrats/myContracts").hasRole("CLIENT")

                        // 👤 CLIENT - Gestion des claims
                        .requestMatchers("/claims/addClaim").hasRole("CLIENT")
                        .requestMatchers("/claims/updateClaim").hasRole("CLIENT")
                        .requestMatchers("/claims/deleteClaim/**").hasRole("CLIENT")
                        .requestMatchers("/claims/getClaim/**").hasAnyRole("CLIENT", "AGENT_ASSURANCE", "ADMIN")
                        .requestMatchers("/claims/allClaim").hasAnyRole("AGENT_ASSURANCE", "ADMIN" , "CLIENT")
                        .requestMatchers("/claims/approve/**").hasRole("AGENT_ASSURANCE")
                        .requestMatchers("/claims/reject/**").hasRole("AGENT_ASSURANCE")
                        .requestMatchers("/claims/calculate-compensation/**").hasAnyRole("CLIENT", "AGENT_ASSURANCE", "ADMIN")
                        .requestMatchers("/claims/stats").hasRole("ADMIN")
                        .requestMatchers("/claims/fraud/**").hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        .requestMatchers("/claims/risk-score/**").hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        .requestMatchers("/claims/recommendation/**").hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        .requestMatchers("/claims/search").hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        .requestMatchers("/claims/prediction/**").hasRole("ADMIN")

                        // 👤 CLIENT - Gestion des documents
                        .requestMatchers("/documents/addDoc").hasRole("CLIENT")
                        .requestMatchers("/documents/updateDoc").hasRole("CLIENT")
                        .requestMatchers("/documents/deleteDoc/**").hasRole("CLIENT")
                        .requestMatchers("/documents/getDoc/**").hasAnyRole("CLIENT", "AGENT_ASSURANCE", "ADMIN")
                        .requestMatchers("/documents/allDoc").hasAnyRole("CLIENT","AGENT_ASSURANCE","ADMIN")
                        .requestMatchers("/documents/claim/**").hasAnyRole("CLIENT", "AGENT_ASSURANCE", "ADMIN")

                        // 💰 Paiements
                        .requestMatchers("/payments/addPayment").hasAnyRole("CLIENT", "AGENT_ASSURANCE", "ADMIN")
                        .requestMatchers("/payments/getPayment/**").hasAnyRole("CLIENT", "AGENT_ASSURANCE", "ADMIN")
                        .requestMatchers("/payments/allPayments").hasAnyRole("CLIENT","AGENT_ASSURANCE","ADMIN")
                        .requestMatchers("/payments/contract/**").hasAnyRole("CLIENT", "AGENT_ASSURANCE", "ADMIN")
                        .requestMatchers("/payments/create-payment-intent/**").hasAnyRole("CLIENT", "AGENT_ASSURANCE", "ADMIN")
                        .requestMatchers("/payments/webhook").permitAll() // Webhook Stripe (public)


                        .requestMatchers("/payments/stripe/webhook").permitAll()

                        // ⚠️ RiskClaims
                        .requestMatchers("/riskclaims/**").hasRole("ADMIN")

                        // Scoring
                        // ✅ CORRECTION: Scoring endpoints avec tous les rôles nécessaires
                        .requestMatchers("/api/scoring/client/**").hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        .requestMatchers("/api/scoring/claim/**").hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        .requestMatchers("/api/scoring/claim/*/advanced").hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        .requestMatchers("/api/scoring/claim/*/auto-decision-advanced").hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        .requestMatchers("/api/scoring/claim/*/detailed-analysis").hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        // ========== CREDIT SCORING ENDPOINTS ==========
                        .requestMatchers("/Scoring/calculate/**").hasAnyRole("ADMIN", "AGENT_FINANCE")
                        .requestMatchers("/Scoring/analyze/**").hasAnyRole("ADMIN", "AGENT_FINANCE")
                        .requestMatchers("/Scoring/quick-score/**").hasAnyRole("ADMIN", "AGENT_FINANCE")

                        // Compensations endpoints
                        // ========== COMPENSATIONS ENDPOINTS ==========
                        .requestMatchers("/compensations/addComp").hasRole("ADMIN")
                        .requestMatchers("/compensations/updateComp").hasRole("ADMIN")
                        .requestMatchers("/compensations/deleteComp/**").hasRole("ADMIN")
                        .requestMatchers("/compensations/getComp/**").hasAnyRole("ADMIN", "AGENT_ASSURANCE", "CLIENT")
                        .requestMatchers("/compensations/allComp").hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        .requestMatchers("/compensations/{id}/pay").hasAnyRole("ADMIN", "AGENT_ASSURANCE", "CLIENT")
                        .requestMatchers("/compensations/recalculate/{claimId}").hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        .requestMatchers("/compensations/{id}/details").hasAnyRole("ADMIN", "AGENT_ASSURANCE", "CLIENT")
                        .requestMatchers("/compensations/{id}/with-scoring").hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        .requestMatchers("/compensations/my-compensations").hasAnyRole("ADMIN", "AGENT_ASSURANCE", "CLIENT")
                        .requestMatchers("/compensations/agent/compensations").hasAnyRole("AGENT_ASSURANCE", "ADMIN")
                        .requestMatchers("/compensations/agent/clients").hasAnyRole("AGENT_ASSURANCE", "ADMIN")

                        // ========== CREDIT ENDPOINTS ==========
                        .requestMatchers("/Credit/addCredit").hasRole("ADMIN")
                        .requestMatchers("/Credit/updateCredit").hasRole("ADMIN")
                        .requestMatchers("/Credit/deleteCredit/**").hasRole("ADMIN")
                        .requestMatchers("/Credit/getCredit/**").hasRole("ADMIN")
                        .requestMatchers("/Credit/approve/**").hasAnyRole("AGENT_FINANCE", "ADMIN")
                        .requestMatchers("/Credit/reject/**").hasAnyRole("AGENT_FINANCE", "ADMIN")
                        .requestMatchers("/Credit/allCredit").hasAnyRole("AGENT_FINANCE", "ADMIN")
                        .requestMatchers("/Credit/closedCreditsWithAverage/**").hasAnyRole("AGENT_FINANCE", "ADMIN")
                        .requestMatchers("/Credit/myCredits").hasRole("CLIENT")

                        // ========== REPAYMENT ENDPOINTS ==========
                        .requestMatchers("/Repayment/pay-credit/**").hasRole("CLIENT")
                        .requestMatchers("/Repayment/myPayments").hasRole("CLIENT")
                        .requestMatchers("/Repayment/remaining/**").authenticated()

                        .requestMatchers("/Repayment/credits/{creditId}/amortissement/pdf").hasAnyRole("CLIENT", "ADMIN")

                        // Agent Finance et Admin

                        .requestMatchers("/Repayment/history/**").hasAnyRole("AGENT_FINANCE", "ADMIN")
                        .requestMatchers("/Repayment/allRepayment").hasAnyRole("AGENT_FINANCE", "ADMIN")
                        .requestMatchers("/Repayment/addRepayment").hasRole("ADMIN")
                        .requestMatchers("/Repayment/updateRepayment").hasRole("ADMIN")
                        .requestMatchers("/Repayment/deleteRepayment/**").hasRole("ADMIN")
                        .requestMatchers("/Repayment/getRepayment/**").hasRole("ADMIN")


                        // ========== NEWS ENDPOINTS ==========
                        // CRUD - Admin uniquement
                        .requestMatchers(HttpMethod.POST, "/api/v1/news").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/news/update/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/news/delete/**").hasRole("ADMIN")

                        // ✅ Upload image - Admin uniquement (CORRIGÉ)
                        .requestMatchers(HttpMethod.POST, "/api/v1/news/*/upload-image")
                        .hasAnyRole("ADMIN", "AGENT_ASSURANCE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/news/*/image")
                        .hasRole("ADMIN")

                        // ========== COMPLAINT ENDPOINTS ==========
                        .requestMatchers("/complaints/addComplaint").hasRole("CLIENT")
                        .requestMatchers("/complaints/updateComplaint/**").hasAnyRole("AGENT_ASSURANCE", "AGENT_FINANCE", "ADMIN", "CLIENT")
                        .requestMatchers("/complaints/deleteComplaint/**").hasRole("ADMIN")
                        .requestMatchers("/complaints/*").hasAnyRole("CLIENT", "AGENT_ASSURANCE", "AGENT_FINANCE", "ADMIN")
                        .requestMatchers("/complaints/all").hasAnyRole("AGENT_ASSURANCE", "AGENT_FINANCE", "ADMIN", "CLIENT")
                        .requestMatchers("/complaints/search").hasAnyRole("AGENT_ASSURANCE", "AGENT_FINANCE", "ADMIN", "CLIENT")
                        .requestMatchers("/complaints/kpi/**").hasAnyRole("AGENT_ASSURANCE", "AGENT_FINANCE", "ADMIN")


                        // Toute autre requête nécessite une authentification
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                // SUPPRIMEZ la ligne duplicate addFilterBefore et corrigez oauth2Login
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler((request, response, authentication) -> {
                            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
                            String email = oauthUser.getAttribute("email");
                            String firstName = oauthUser.getAttribute("given_name");
                            String lastName = oauthUser.getAttribute("family_name");
                            String picture = oauthUser.getAttribute("picture");

                            // Find OR create — never throws
                            User dbUser = userRepository.findByEmail(email).orElseGet(() -> {
                                Client newClient = new Client();
                                newClient.setEmail(email);
                                newClient.setFirstName(firstName != null ? firstName : "");
                                newClient.setLastName(lastName != null ? lastName : "");
                                newClient.setPhoto(picture);
                                newClient.setRole(Role.CLIENT);
                                newClient.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                                newClient.setTelephone("");
                                return userRepository.save(newClient);
                            });

                            String token = jwtUtils.generateToken(dbUser);
                            response.sendRedirect("http://localhost:4200/oauth2/callback?token=" + token);
                        })
                        .failureHandler((request, response, exception) -> {
                            System.err.println("OAuth2 failed: " + exception.getMessage());
                            response.sendRedirect("http://localhost:4200/?error=oauth_failed");
                        })
                );

        return http.build();
    }
}