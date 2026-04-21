package org.example.projet_pi.Service;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.Repository.*;
import org.example.projet_pi.entity.LoginHistory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final ClientRepository clientRepository;
    private final AgentAssuranceRepository agentAssuranceRepository;
    private final AgentFinanceRepository agentFinanceRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalClients = clientRepository.count();
        long totalAgentsAssurance = agentAssuranceRepository.count();
        long totalAgentsFinance = agentFinanceRepository.count();
        long totalAdmins = adminRepository.count();

        stats.put("totalUsers", totalUsers);
        stats.put("totalClients", totalClients);
        stats.put("totalAgentsAssurance", totalAgentsAssurance);
        stats.put("totalAgentsFinance", totalAgentsFinance);
        stats.put("totalAdmins", totalAdmins);
        stats.put("connectionStats", getConnectionStatsForPeriod(7));
        stats.put("recentActivities", getRecentActivities());

        return stats;
    }

    public Map<String, Object> getConnectionStatsForPeriod(int days) {
        Map<String, Object> connectionStats = new HashMap<>();

        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, -days);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startDate = calendar.getTime();

        long weekdayCount = loginHistoryRepository.countWeekdayConnectionsNative(startDate, endDate);
        long weekendCount = loginHistoryRepository.countWeekendConnectionsNative(startDate, endDate);
        long totalConnections = weekdayCount + weekendCount;

        double weekdayPercentage = totalConnections > 0 ? (weekdayCount * 100.0 / totalConnections) : 0;
        double weekendPercentage = totalConnections > 0 ? (weekendCount * 100.0 / totalConnections) : 0;

        connectionStats.put("weekdayConnections", weekdayCount);
        connectionStats.put("weekendConnections", weekendCount);
        connectionStats.put("totalConnections", totalConnections);
        connectionStats.put("weekdayPercentage", Math.round(weekdayPercentage * 10) / 10.0);
        connectionStats.put("weekendPercentage", Math.round(weekendPercentage * 10) / 10.0);
        connectionStats.put("startDate", startDate);
        connectionStats.put("endDate", endDate);
        connectionStats.put("periodDays", days);

        return connectionStats;
    }

    public Map<String, Object> getCurrentMonthConnectionStats() {
        Map<String, Object> connectionStats = new HashMap<>();

        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startDate = calendar.getTime();

        long weekdayCount = loginHistoryRepository.countWeekdayConnectionsNative(startDate, endDate);
        long weekendCount = loginHistoryRepository.countWeekendConnectionsNative(startDate, endDate);
        long totalConnections = weekdayCount + weekendCount;

        double weekdayPercentage = totalConnections > 0 ? (weekdayCount * 100.0 / totalConnections) : 0;
        double weekendPercentage = totalConnections > 0 ? (weekendCount * 100.0 / totalConnections) : 0;

        connectionStats.put("weekdayConnections", weekdayCount);
        connectionStats.put("weekendConnections", weekendCount);
        connectionStats.put("totalConnections", totalConnections);
        connectionStats.put("weekdayPercentage", Math.round(weekdayPercentage * 10) / 10.0);
        connectionStats.put("weekendPercentage", Math.round(weekendPercentage * 10) / 10.0);
        connectionStats.put("startDate", startDate);
        connectionStats.put("endDate", endDate);

        return connectionStats;
    }

    public Map<String, Object> getConnectionDetails(int page, int size) {
        Map<String, Object> details = new HashMap<>();

        Pageable pageable = PageRequest.of(page, size);
        var loginHistoryPage = loginHistoryRepository.findAll(pageable);

        details.put("connections", loginHistoryPage.getContent());
        details.put("totalElements", loginHistoryPage.getTotalElements());
        details.put("totalPages", loginHistoryPage.getTotalPages());
        details.put("currentPage", page);
        details.put("pageSize", size);

        return details;
    }

    // ✅ CORRIGÉ: Une connexion par utilisateur distinct
    public List<Map<String, Object>> getConnectionLocations() {
        // ✅ Utiliser la nouvelle requête — dernière connexion par user
        List<LoginHistory> histories = loginHistoryRepository.findLastConnectionPerUser();

        return histories.stream()
                .map(h -> {
                    Map<String, Object> loc = new HashMap<>();

                    // ✅ Email depuis User ou directement
                    String email = h.getUser() != null ?
                            h.getUser().getEmail() :
                            (h.getEmail() != null ? h.getEmail() : "Unknown");

                    loc.put("email", email);
                    loc.put("lat", h.getLatitude());
                    loc.put("lon", h.getLongitude());
                    loc.put("city", h.getCity() != null ? h.getCity() : "Unknown");
                    loc.put("country", h.getCountry() != null ? h.getCountry() : "Unknown");
                    loc.put("date", h.getLoginTime());
                    return loc;
                })
                .collect(Collectors.toList());
    }

    private List<String> getRecentActivities() {
        return List.of(
                "Dashboard consulté",
                "Système opérationnel"
        );
    }
}