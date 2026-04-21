// Controller/AdminStatsController.java (Amélioré)
package org.example.projet_pi.Controller;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.LoginHistoryRepository;
import org.example.projet_pi.Service.AdminStatsService;
import org.example.projet_pi.entity.LoginHistory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;
    private final LoginHistoryRepository loginHistoryRepository;

    // Endpoint existant - Dashboard complet
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getDashboardStats() {
        return adminStatsService.getDashboardStats();
    }

    // ✅ NOUVEAU: Endpoint pour les statistiques de connexion uniquement
    @GetMapping("/connection-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getConnectionStats(
            @RequestParam(defaultValue = "7") int days) {

        Map<String, Object> stats = adminStatsService.getConnectionStatsForPeriod(days);
        return ResponseEntity.ok(stats);
    }

    // ✅ NOUVEAU: Endpoint pour les statistiques de connexion (7 derniers jours par défaut)
    @GetMapping("/connection-stats/last7days")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getLast7DaysConnectionStats() {
        Map<String, Object> stats = adminStatsService.getConnectionStatsForPeriod(7);
        return ResponseEntity.ok(stats);
    }

    // ✅ NOUVEAU: Endpoint pour les statistiques de connexion (30 derniers jours)
    @GetMapping("/connection-stats/last30days")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getLast30DaysConnectionStats() {
        Map<String, Object> stats = adminStatsService.getConnectionStatsForPeriod(30);
        return ResponseEntity.ok(stats);
    }

    // ✅ NOUVEAU: Endpoint pour les statistiques de connexion du mois en cours
    @GetMapping("/connection-stats/current-month")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCurrentMonthConnectionStats() {
        Map<String, Object> stats = adminStatsService.getCurrentMonthConnectionStats();
        return ResponseEntity.ok(stats);
    }

    // ✅ NOUVEAU: Endpoint pour les détails des connexions (avec pagination optionnelle)
    @GetMapping("/connection-details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getConnectionDetails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> details = adminStatsService.getConnectionDetails(page, size);
        return ResponseEntity.ok(details);
    }

    // ✅ NOUVEAU: Endpoint pour le résumé complet (dashboard amélioré)
    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Stats utilisateurs
        summary.put("userStats", adminStatsService.getDashboardStats());

        // Stats connexions (7 jours)
        summary.put("connectionStats7Days", adminStatsService.getConnectionStatsForPeriod(7));

        // Stats connexions (30 jours)
        summary.put("connectionStats30Days", adminStatsService.getConnectionStatsForPeriod(30));

        return ResponseEntity.ok(summary);
    }
    // Dans AdminStatsController.java
    @GetMapping("/connection-locations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getConnectionLocations() {
        List<Map<String, Object>> locations = adminStatsService.getConnectionLocations();
        return ResponseEntity.ok(locations);
    }
}