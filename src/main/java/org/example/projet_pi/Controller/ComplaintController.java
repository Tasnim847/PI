package org.example.projet_pi.Controller;

import org.example.projet_pi.Dto.ComplaintSearchDTO;
import org.example.projet_pi.Service.IComplaintService;
import org.example.projet_pi.entity.Complaint;
import org.example.projet_pi.entity.User;
import org.example.projet_pi.Repository.ComplaintRepository;
import org.example.projet_pi.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ComplaintController implements IComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public ComplaintController(ComplaintRepository complaintRepository, UserRepository userRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
    }

    // ---------------- CRUD ----------------

    @Override
    @Transactional
    public Complaint addComplaint(Complaint complaint) {
        // 🔴 CORRECTION : Vérifier et charger les utilisateurs avant de sauvegarder
        validateAndLoadUsers(complaint);
        return complaintRepository.save(complaint);
    }

    @Override
    @Transactional
    public Complaint updateComplaint(Complaint complaint) {
        // Vérifie que la réclamation existe avant mise à jour
        Complaint existing = complaintRepository.findById(complaint.getId())
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        existing.setStatus(complaint.getStatus());
        existing.setMessage(complaint.getMessage());
        existing.setClaimDate(complaint.getClaimDate());
        existing.setResolutionDate(complaint.getResolutionDate());

        // 🔴 CORRECTION : Vérifier et charger les utilisateurs avant mise à jour
        if (complaint.getAgentAssurance() != null) {
            validateAndLoadUsers(complaint);
            existing.setAgentAssurance(complaint.getAgentAssurance());
        }
        if (complaint.getAgentFinance() != null) {
            existing.setAgentFinance(complaint.getAgentFinance());
        }
        if (complaint.getClient() != null) {
            existing.setClient(complaint.getClient());
        }

        return complaintRepository.save(existing);
    }

    // 🔴 NOUVELLE MÉTHODE : Valider et charger les utilisateurs
    private void validateAndLoadUsers(Complaint complaint) {
        // Vérifier et charger l'agent d'assurance
        if (complaint.getAgentAssurance() != null && complaint.getAgentAssurance().getId() != null) {
            User agentAssurance = userRepository.findById(complaint.getAgentAssurance().getId())
                    .orElseThrow(() -> new RuntimeException("Agent Assurance avec ID " +
                            complaint.getAgentAssurance().getId() + " n'existe pas"));
            complaint.setAgentAssurance(agentAssurance);
        } else {
            complaint.setAgentAssurance(null);
        }

        // Vérifier et charger l'agent financier
        if (complaint.getAgentFinance() != null && complaint.getAgentFinance().getId() != null) {
            User agentFinance = userRepository.findById(complaint.getAgentFinance().getId())
                    .orElseThrow(() -> new RuntimeException("Agent Finance avec ID " +
                            complaint.getAgentFinance().getId() + " n'existe pas"));
            complaint.setAgentFinance(agentFinance);
        } else {
            complaint.setAgentFinance(null);
        }

        // Vérifier et charger le client (obligatoire)
        if (complaint.getClient() == null || complaint.getClient().getId() == null) {
            throw new RuntimeException("Le client est obligatoire");
        }

        User client = userRepository.findById(complaint.getClient().getId())
                .orElseThrow(() -> new RuntimeException("Client avec ID " +
                        complaint.getClient().getId() + " n'existe pas"));
        complaint.setClient(client);
    }

    @Override
    @Transactional
    public void deleteComplaint(Long id) {
        complaintRepository.deleteById(id);
    }

    @Override
    public Complaint getComplaintById(Long id) {
        return complaintRepository.findById(id).orElse(null);
    }

    @Override
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    // ---------------- Recherche avancée ----------------

    @Override
    public List<Complaint> searchComplaints(ComplaintSearchDTO dto) {
        return complaintRepository.searchComplaints(
                dto.getStatus(),
                dto.getKeyword(),
                dto.getClientId(),
                dto.getAgentAssuranceId(),
                dto.getAgentFinanceId(),
                dto.getDateDebut(),
                dto.getDateFin()
        );
    }

    // ---------------- KPI ----------------

    // 1️⃣ Temps moyen de traitement (en jours)
    @Override
    public double calculateAverageProcessingTime() {
        List<Complaint> closed = complaintRepository.findByStatus("CLOSED");
        if (closed.isEmpty()) return 0;

        long totalDays = 0;
        int count = 0;

        for (Complaint c : closed) {
            if (c.getClaimDate() != null && c.getResolutionDate() != null) {
                long days = ChronoUnit.DAYS.between(
                        c.getClaimDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        c.getResolutionDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                );
                totalDays += days;
                count++;
            }
        }

        return count == 0 ? 0 : totalDays / (double) count;
    }

    // 2️⃣ Taux de résolution
    @Override
    public double resolutionRate() {
        long total = complaintRepository.count();
        if (total == 0) return 0;

        long resolved = complaintRepository.countByStatus("APPROVED");
        return (resolved * 100.0) / total;
    }

    // 3️⃣ Taux de rejet
    @Override
    public double rejectionRate() {
        long total = complaintRepository.count();
        if (total == 0) return 0;

        long rejected = complaintRepository.countByStatus("REJECTED");
        return (rejected * 100.0) / total;
    }

    // 4️⃣ Agent le plus performant
    @Override
    public String findTopAgent() {
        List<Complaint> complaints = complaintRepository.findAll();
        Map<String, Long> agentCount = new HashMap<>();

        for (Complaint c : complaints) {
            if (c.getAgentAssurance() != null) {
                String name = c.getAgentAssurance().getFirstName() + " (Assurance)";
                agentCount.put(name, agentCount.getOrDefault(name, 0L) + 1);
            }
            if (c.getAgentFinance() != null) {
                String name = c.getAgentFinance().getFirstName() + " (Finance)";
                agentCount.put(name, agentCount.getOrDefault(name, 0L) + 1);
            }
        }

        return agentCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No Agent");
    }

    // 5️⃣ Dashboard KPI complet
    @Override
    public Map<String, Object> getDashboardKpi() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("averageProcessingTime", calculateAverageProcessingTime());
        dashboard.put("resolutionRate", resolutionRate());
        dashboard.put("rejectionRate", rejectionRate());
        dashboard.put("topAgent", findTopAgent());
        return dashboard;
    }
}