package org.example.projet_pi.Controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.LoginHistoryRepository;
import org.example.projet_pi.Repository.UserRepository;
import org.example.projet_pi.Service.EmailService2;
import org.example.projet_pi.Service.SmsServiceYosr;
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
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final EmailService2 emailService;
    private final SmsServiceYosr smsServiceYosr;
    private final LoginHistoryRepository loginHistoryRepository;

    private static final double[][] TUNISIAN_CITIES_COORDS = {
            {36.8065, 10.1815}, {36.7219, 10.7260}, {35.6762, 10.8394},
            {33.8815, 10.0982}, {37.2744, 9.8739}, {36.4610, 10.7348},
            {35.5004, 11.0489}, {36.8625, 10.1956}, {35.8245, 10.6346},
            {36.0450, 9.3700}, {34.7406, 10.7603}, {37.0741, 10.5648}
    };

    private static final String[] TUNISIAN_CITY_NAMES = {
            "Tunis", "Sfax", "Sousse", "Gabès", "Bizerte",
            "Nabeul", "Mahdia", "La Marsa", "Monastir",
            "Siliana", "Sfax Sud", "Hammamet"
    };

    public String getClientIP(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String getRealPublicIP() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String ip = restTemplate.getForObject("https://api.ipify.org", String.class);
            return ip;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> getLocation(String ip) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://ip-api.com/json/" + ip +
                    "?fields=status,city,country,lat,lon,regionName";
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void setLocation(LoginHistory history, User user, String ip) {
        boolean isLocalhost = ip == null ||
                ip.equals("127.0.0.1") ||
                ip.equals("0:0:0:0:0:0:0:1") ||
                ip.equals("::1");

        if (isLocalhost) {

            String publicIp = getRealPublicIP();

            if (publicIp != null && !publicIp.isEmpty()) {
                try {
                    Map<String, Object> location = getLocation(publicIp);
                    if (location != null && "success".equals(location.get("status"))) {
                        history.setCity((String) location.get("city"));
                        history.setCountry((String) location.get("country"));
                        history.setLatitude(Double.valueOf(location.get("lat").toString()));
                        history.setLongitude(Double.valueOf(location.get("lon").toString()));
                        return;
                    }
                } catch (Exception ignored) {}
            }

            int cityIndex = (int)(user.getId() % TUNISIAN_CITIES_COORDS.length);
            history.setLatitude(TUNISIAN_CITIES_COORDS[cityIndex][0]);
            history.setLongitude(TUNISIAN_CITIES_COORDS[cityIndex][1]);
            history.setCity(TUNISIAN_CITY_NAMES[cityIndex]);
            history.setCountry("Tunisia");

        } else {
            try {
                Map<String, Object> location = getLocation(ip);
                if (location != null && "success".equals(location.get("status"))) {
                    history.setCity((String) location.get("city"));
                    history.setCountry((String) location.get("country"));
                    history.setLatitude(Double.valueOf(location.get("lat").toString()));
                    history.setLongitude(Double.valueOf(location.get("lon").toString()));
                } else {
                    history.setCity("Tunis");
                    history.setCountry("Tunisia");
                    history.setLatitude(36.8065);
                    history.setLongitude(10.1815);
                }
            } catch (Exception e) {
                history.setCity("Tunis");
                history.setCountry("Tunisia");
                history.setLatitude(36.8065);
                history.setLongitude(10.1815);
            }
        }
    }

    @PostMapping(value = "/register", consumes = "multipart/form-data")
    public ResponseEntity<?> register(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "telephone", required = false) String telephone,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) {
        try {
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body("Email already exists");
            }

            String fileName = null;
            if (photo != null && !photo.isEmpty()) {
                fileName = uploadPhoto(photo);
            }

            String encodedPassword = passwordEncoder.encode(password);

            Client client = new Client();
            client.setFirstName(firstName);
            client.setLastName(lastName);
            client.setEmail(email);
            client.setPassword(encodedPassword);
            client.setTelephone(telephone != null && !telephone.isEmpty() ? telephone : null);
            client.setRole(Role.CLIENT);
            client.setPhoto(fileName);

            User savedUser = userRepository.save(client);

            // ✅ Email non bloquant
            try {
                emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName());
                System.out.println("✅ Email envoyé");
            } catch (Exception e) {
                System.err.println("⚠️ Erreur email: " + e.getMessage());
            }

            // ✅ SMS non bloquant
            try {
                if (savedUser.getTelephone() != null &&
                        !savedUser.getTelephone().isEmpty() &&
                        savedUser.getTelephone().matches("^\\+216\\d{8}$")) {
                    smsServiceYosr.sendSms(savedUser.getTelephone(), savedUser.getFirstName());
                    System.out.println("✅ SMS envoyé");
                } else {
                    System.out.println("⚠️ SMS ignoré - téléphone absent ou invalide");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur SMS: " + e.getMessage());
            }

            return ResponseEntity.ok("User registered successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User userRequest,
                                   HttpServletRequest request) {

        String ip = getClientIP(request);

        try {
            User user = userRepository.findByEmail(userRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.isAccountNonLocked()) {
                long lockDuration = 2 * 60 * 1000;

                // ✅ CORRECTION ICI
                if (user.getLockTime() != null &&
                        user.getLockTime().getTime() + lockDuration < System.currentTimeMillis()) {

                    user.setAccountNonLocked(true);
                    user.setLoginAttempts(0);
                    user.setLockTime(null);
                    userRepository.save(user);

                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Votre compte est bloqué. Réessayez après 2 minutes.");
                }
            }

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userRequest.getEmail(),
                            userRequest.getPassword()
                    )
            );

            user.setLoginAttempts(0);
            userRepository.save(user);

            LoginHistory history = new LoginHistory();
            history.setUser(user);
            history.setLoginTime(new Date());
            history.setIpAddress(ip);

            setLocation(history, user, ip);
            loginHistoryRepository.save(history);

            String token = jwtUtils.generateToken(user);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", user.getRole().name());

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {

            User user = userRepository.findByEmail(userRequest.getEmail()).orElse(null);

            if (user != null) {
                int attempts = user.getLoginAttempts() + 1;
                user.setLoginAttempts(attempts);

                if (attempts >= 5) {
                    user.setAccountNonLocked(false);
                    user.setLockTime(new Date());

                    LoginHistory failedHistory = new LoginHistory();
                    failedHistory.setUser(user);
                    failedHistory.setLoginTime(new Date());
                    failedHistory.setIpAddress(ip);
                    failedHistory.setCity("Failed Login");
                    failedHistory.setCountry("Unknown");
                    loginHistoryRepository.save(failedHistory);

                    smsServiceYosr.sendSms(
                            user.getTelephone(),
                            "Votre compte est bloqué pour 2 minutes."
                    );
                }

                userRepository.save(user);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Mot de passe incorrect");
        }
    }

    @PostMapping("/unlock-user/{id}")
    public ResponseEntity<?> unlockUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAccountNonLocked(true);
        user.setLoginAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);
        return ResponseEntity.ok("User unlocked");
    }

    @GetMapping("/login-history/{userId}")
    public ResponseEntity<List<LoginHistory>> getHistory(@PathVariable Long userId) {
        List<LoginHistory> history = loginHistoryRepository.findByUserId(userId);
        return ResponseEntity.ok(history);
    }

    private String uploadPhoto(MultipartFile file) {
        try {
            String uploadDir = "uploads/";
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'upload de la photo");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("email", user.getEmail());
        response.put("telephone", user.getTelephone());
        response.put("role", user.getRole().name());
        response.put("photo", user.getPhoto());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/update-me")
    public ResponseEntity<?> updateMyProfile(
            Authentication authentication,
            @RequestBody Map<String, String> request
    ) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.get("firstName") != null)
            user.setFirstName(request.get("firstName"));
        if (request.get("lastName") != null)
            user.setLastName(request.get("lastName"));
        if (request.get("telephone") != null)
            user.setTelephone(request.get("telephone"));

        userRepository.save(user);
        return ResponseEntity.ok("Profile updated successfully");
    }
}