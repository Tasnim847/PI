package org.example.projet_pi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 🔹 Désactive CSRF pour POST/PUT/DELETE depuis Postman
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // 🔓 Endpoints publics
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/otp/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // 👑 ADMIN uniquement
                        .requestMatchers("/products/addProduct").hasRole("ADMIN")
                        .requestMatchers("/products/updateProduct").hasRole("ADMIN")
                        .requestMatchers("/products/deleteProduct/**").hasRole("ADMIN")
                        .requestMatchers("/admins/**").hasRole("ADMIN")

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

                        // ⚠️ RiskClaims (généralement gérés par le système)

                        .requestMatchers("/riskclaims/**").hasRole("ADMIN")


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

                        // Toute autre requête nécessite une authentification
                        .anyRequest().authenticated()
                )
              .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}