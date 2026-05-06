package org.example.projet_pi.Controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.LoginHistoryRepository;
import org.example.projet_pi.Repository.UserRepository;
import org.example.projet_pi.Service.EmailService2;
import org.example.projet_pi.Service.SmsServiceYosr;
import org.example.projet_pi.config.JwtUtils;
import org.example.projet_pi.entity.*;
import org.example.projet_pi.security.CustomUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

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
        String[] services = {
                "https://api.ipify.org",
                "https://api4.my-ip.io/ip",
                "https://checkip.amazonaws.com"
        };

        for (String service : services) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(2000);
                factory.setReadTimeout(2000);
                restTemplate.setRequestFactory(factory);

                String ip = restTemplate.getForObject(service, String.class);
                if (ip != null && !ip.isBlank()) {
                    System.out.println("🌐 IP publique via " + service + ": " + ip.trim());
                    return ip.trim();
                }
            } catch (Exception e) {
                System.err.println("⚠️ " + service + " timeout: " + e.getMessage());
            }
        }
        return null;
    }

    private Map<String, Object> getLocation(String ip) {
        try {
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }

            RestTemplate restTemplate = new RestTemplate();
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(2000);
            factory.setReadTimeout(2000);
            restTemplate.setRequestFactory(factory);

            String url = "https://ipinfo.io/" + ip + "/json";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.get("city") != null) {
                String loc = (String) response.get("loc");
                double lat = 36.8065;
                double lon = 10.1815;

                if (loc != null && loc.contains(",")) {
                    String[] parts = loc.split(",");
                    lat = Double.parseDouble(parts[0].trim());
                    lon = Double.parseDouble(parts[1].trim());
                }

                String city = (String) response.getOrDefault("city", "Unknown");
                String country = (String) response.getOrDefault("country", "Unknown");

                System.out.println("📍 ipinfo.io: " + city + ", " + country +
                        " [" + lat + ", " + lon + "]");

                Map<String, Object> result = new HashMap<>();
                result.put("status", "success");
                result.put("city", city);
                result.put("country", country);
                result.put("lat", lat);
                result.put("lon", lon);
                return result;
            }

        } catch (Exception e) {
            System.err.println("⚠️ Erreur géolocalisation pour IP " + ip + ": " + e.getMessage());
        }
        return null;
    }

    private void setLocation(LoginHistory history, User user, String ip) {
        boolean isLocalhost = ip == null ||
                ip.equals("127.0.0.1") ||
                ip.equals("0:0:0:0:0:0:0:1") ||
                ip.equals("::1") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                ip.startsWith("172.");

        if (isLocalhost) {
            System.out.println("📍 IP locale détectée: " + ip + ", tentative récupération IP publique...");

            String publicIp = getRealPublicIP();

            if (publicIp != null && !publicIp.isEmpty()) {
                try {
                    Map<String, Object> location = getLocation(publicIp);
                    if (location != null && "success".equals(location.get("status"))) {
                        history.setCity((String) location.get("city"));
                        history.setCountry((String) location.get("country"));
                        history.setLatitude(Double.valueOf(location.get("lat").toString()));
                        history.setLongitude(Double.valueOf(location.get("lon").toString()));
                        System.out.println("✅ Localisation via IP publique: " + history.getCity() + ", " + history.getCountry());
                        return;
                    }
                } catch (Exception ignored) {}
            }

            history.setCity("Local / Unknown");
            history.setCountry("Tunisia");
            history.setLatitude(36.8065);
            history.setLongitude(10.1815);
            System.out.println("⚠️ IP publique non disponible, coordonnées par défaut (Tunis)");

        } else {
            try {
                System.out.println("📍 IP distante: " + ip);
                Map<String, Object> location = getLocation(ip);
                if (location != null && "success".equals(location.get("status"))) {
                    history.setCity((String) location.get("city"));
                    history.setCountry((String) location.get("country"));
                    history.setLatitude(Double.valueOf(location.get("lat").toString()));
                    history.setLongitude(Double.valueOf(location.get("lon").toString()));
                    System.out.println("✅ Localisation réelle: " + history.getCity() + ", " + history.getCountry());
                } else {
                    history.setCity("Tunis");
                    history.setCountry("Tunisia");
                    history.setLatitude(36.8065);
                    history.setLongitude(10.1815);
                    System.out.println("⚠️ Location par défaut: Tunis");
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur setLocation: " + e.getMessage());
                history.setCity("Tunis");
                history.setCountry("Tunisia");
                history.setLatitude(36.8065);
                history.setLongitude(10.1815);
            }
        }

        System.out.println("📌 Coordonnées finales: " + history.getLatitude() + ", " + history.getLongitude());
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

            try {
                emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName());
                System.out.println("✅ Email envoyé");
            } catch (Exception e) {
                System.err.println("⚠️ Erreur email: " + e.getMessage());
            }

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
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request) {

        String ip = getClientIP(request);
        System.out.println("🔐 Login pour: " + loginRequest.getEmail() + " | IP: " + ip);

        try {
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.isAccountNonLocked()) {
                long lockDuration = 2 * 60 * 1000;
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
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            user.setLoginAttempts(0);
            userRepository.save(user);

            LoginHistory history = new LoginHistory();
            history.setUser(user);
            history.setEmail(user.getEmail());
            history.setLoginTime(new Date());
            history.setIpAddress(ip);

            // ✅ PRIORITÉ AUX COORDONNÉES GPS DU NAVIGATEUR
            if (loginRequest.getClientLat() != null && loginRequest.getClientLon() != null) {
                history.setLatitude(loginRequest.getClientLat());
                history.setLongitude(loginRequest.getClientLon());
                history.setCountry("Tunisia");

                // ✅ Récupérer le vrai nom de la ville automatiquement
                String cityName = getCityFromCoordinates(loginRequest.getClientLat(), loginRequest.getClientLon());
                history.setCity(cityName != null && !cityName.isEmpty() ? cityName : "Localisation GPS");

                System.out.println("✅ Position GPS: " + history.getCity() +
                        " (" + loginRequest.getClientLat() + ", " + loginRequest.getClientLon() + ")");
            } else {
                // Fallback: géolocalisation par IP
                setLocation(history, user, ip);
            }

            loginHistoryRepository.save(history);

            String token = jwtUtils.generateToken(user);
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", user.getRole().name());

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            User user = userRepository.findByEmail(loginRequest.getEmail()).orElse(null);

            if (user != null) {
                int attempts = user.getLoginAttempts() + 1;
                user.setLoginAttempts(attempts);

                if (attempts >= 5) {
                    user.setAccountNonLocked(false);
                    user.setLockTime(new Date());

                    LoginHistory failedHistory = new LoginHistory();
                    failedHistory.setUser(user);
                    failedHistory.setEmail(user.getEmail());
                    failedHistory.setLoginTime(new Date());
                    failedHistory.setIpAddress(ip);
                    failedHistory.setCity("Failed Login");
                    failedHistory.setCountry("Unknown");
                    loginHistoryRepository.save(failedHistory);

                    try {
                        smsServiceYosr.sendSms(user.getTelephone(),
                                "Votre compte est bloqué pour 2 minutes.");
                    } catch (Exception smsEx) {
                        System.err.println("⚠️ SMS non envoyé: " + smsEx.getMessage());
                    }
                }
                userRepository.save(user);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Mot de passe incorrect");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur interne du serveur");
        }
    }

    // ✅ MÉTHODE UNIVERSELLE - Fonctionne pour n'importe quelle ville en Tunisie
    private String getCityFromCoordinates(double lat, double lon) {
        String city = null;

        // Essai 1: API BigDataCloud (gratuit, précis, sans clé)
        try {
            String url = String.format("https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=%f&longitude=%f&localityLanguage=fr", lat, lon);

            RestTemplate restTemplate = new RestTemplate();
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(5000);
            restTemplate.setRequestFactory(factory);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                if (response.containsKey("city") && response.get("city") != null) {
                    city = (String) response.get("city");
                } else if (response.containsKey("town") && response.get("town") != null) {
                    city = (String) response.get("town");
                } else if (response.containsKey("village") && response.get("village") != null) {
                    city = (String) response.get("village");
                } else if (response.containsKey("locality") && response.get("locality") != null) {
                    city = (String) response.get("locality");
                }

                if (city != null && !city.isEmpty()) {
                    System.out.println("🏙️ BigDataCloud -> Ville: " + city);
                    return city;
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ BigDataCloud: " + e.getMessage());
        }

        // Essai 2: Nominatim (OpenStreetMap)
        try {
            String url = String.format("https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&zoom=18&addressdetails=1&accept-language=fr", lat, lon);

            RestTemplate restTemplate = new RestTemplate();
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(5000);
            restTemplate.setRequestFactory(factory);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TunisiaMapApp/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("address")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> address = (Map<String, Object>) responseBody.get("address");

                if (address.containsKey("city")) city = (String) address.get("city");
                else if (address.containsKey("town")) city = (String) address.get("town");
                else if (address.containsKey("village")) city = (String) address.get("village");
                else if (address.containsKey("municipality")) city = (String) address.get("municipality");

                if (city != null && !city.isEmpty()) {
                    System.out.println("🏙️ Nominatim -> Ville: " + city);
                    return city;
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Nominatim: " + e.getMessage());
        }

        // Essai 3: Fallback - Déterminer la région à partir des coordonnées
        String fallbackCity = getRegionFromCoordinates(lat, lon);
        System.out.println("🏙️ Fallback -> Ville: " + fallbackCity);
        return fallbackCity;
    }

    // ✅ Méthode de fallback basée sur les régions tunisiennes
    private String getRegionFromCoordinates(double lat, double lon) {
        // Déterminer la région selon les coordonnées
        if (lat >= 36.5 && lat <= 37.5 && lon >= 9.5 && lon <= 10.5) return "Tunis";
        if (lat >= 36.5 && lat <= 37.5 && lon >= 10.5 && lon <= 11.0) return "Bizerte";
        if (lat >= 35.5 && lat <= 36.5 && lon >= 10.0 && lon <= 10.8) return "Sousse";
        if (lat >= 35.5 && lat <= 36.5 && lon >= 10.8 && lon <= 11.2) return "Monastir";
        if (lat >= 35.0 && lat <= 35.8 && lon >= 10.5 && lon <= 11.2) return "Mahdia";
        if (lat >= 34.5 && lat <= 35.5 && lon >= 10.0 && lon <= 11.0) return "Sfax";
        if (lat >= 33.5 && lat <= 34.5 && lon >= 9.5 && lon <= 10.5) return "Gabès";
        if (lat >= 36.0 && lat <= 37.0 && lon >= 8.5 && lon <= 9.5) return "Le Kef";
        if (lat >= 35.0 && lat <= 36.0 && lon >= 9.0 && lon <= 10.0) return "Kairouan";
        if (lat >= 36.0 && lat <= 37.0 && lon >= 10.0 && lon <= 11.0) return "Nabeul";

        return "Tunisie";
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

        // ✅ Utiliser le Principal directement (sans requête SQL !)
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserPrincipal) {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) principal;

            Map<String, Object> response = new HashMap<>();
            response.put("id", userPrincipal.getId());
            response.put("firstName", userPrincipal.getFirstName());
            response.put("lastName", userPrincipal.getLastName());
            response.put("email", userPrincipal.getEmail());
            response.put("telephone", userPrincipal.getTelephone());
            response.put("role", userPrincipal.getRole());
            response.put("photo", userPrincipal.getPhoto());
            System.out.println("📸 Photo field: " + userPrincipal.getPhoto());

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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