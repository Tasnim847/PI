package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.ComplaintDTO;
import org.example.projet_pi.Dto.ComplaintSearchDTO;
import org.example.projet_pi.Repository.ComplaintRepository;
import org.example.projet_pi.entity.Complaint;
import org.example.projet_pi.entity.User;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ComplaintService implements IComplaintService {

    private final ComplaintRepository complaintRepository;
    private final SmsService3 smsService3;

    public ComplaintService(ComplaintRepository complaintRepository, SmsService3 smsService3) {
        this.complaintRepository = complaintRepository;
        this.smsService3 = smsService3;
    }

    // ================= DTO -> ENTITY =================
    private Complaint mapToEntity(ComplaintDTO dto) {
        Complaint c = new Complaint();
        c.setId(dto.getId());
        c.setStatus(dto.getStatus());
        c.setMessage(dto.getMessage());
        c.setPhone(dto.getPhone());

        if (dto.getClaimDate() != null)
            c.setClaimDate(java.sql.Timestamp.valueOf(dto.getClaimDate()));

        if (dto.getResolutionDate() != null)
            c.setResolutionDate(java.sql.Timestamp.valueOf(dto.getResolutionDate()));

        return c;
    }

    // ================= ENTITY -> DTO =================
    private ComplaintDTO mapToDTO(Complaint c) {
        ComplaintDTO dto = new ComplaintDTO();
        dto.setId(c.getId());
        dto.setStatus(c.getStatus());
        dto.setMessage(c.getMessage());
        dto.setPhone(c.getPhone());

        if (c.getClaimDate() != null)
            dto.setClaimDate(c.getClaimDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());

        if (c.getResolutionDate() != null)
            dto.setResolutionDate(c.getResolutionDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());

        if (c.getClient() != null)
            dto.setClientId(c.getClient().getId());

        if (c.getAgentAssurance() != null)
            dto.setAgentAssuranceId(c.getAgentAssurance().getId());

        if (c.getAgentFinance() != null)
            dto.setAgentFinanceId(c.getAgentFinance().getId());

        return dto;
    }

    // ================= CRUD =================

    @Override
    public ComplaintDTO addComplaint(ComplaintDTO dto) {
        Complaint complaint = mapToEntity(dto);
        Complaint saved = complaintRepository.save(complaint);

        if (saved.getPhone() != null && !saved.getPhone().isEmpty()) {
            try {
                smsService3.sendSms(
                        saved.getPhone(),
                        "Votre réclamation a été enregistrée avec succès. Elle sera traitée dans les plus brefs délais."
                );
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi du SMS: " + e.getMessage());
            }
        }

        return mapToDTO(saved);
    }

    @Override
    public ComplaintDTO updateComplaint(Long id, ComplaintDTO dto) {
        Complaint existing = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found with id: " + id));

        if (dto.getStatus() != null)
            existing.setStatus(dto.getStatus());

        if (dto.getMessage() != null)
            existing.setMessage(dto.getMessage());

        if (dto.getPhone() != null)
            existing.setPhone(dto.getPhone());

        Complaint updated = complaintRepository.save(existing);
        return mapToDTO(updated);
    }

    @Override
    public void deleteComplaint(Long id) {
        if (!complaintRepository.existsById(id)) {
            throw new RuntimeException("Complaint not found with id: " + id);
        }
        complaintRepository.deleteById(id);
    }

    @Override
    public ComplaintDTO getComplaintById(Long id) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found with id: " + id));
        return mapToDTO(c);
    }

    @Override
    public List<ComplaintDTO> getAllComplaints() {
        return complaintRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // ================= SEARCH =================

    @Override
    public List<ComplaintDTO> searchComplaints(ComplaintSearchDTO dto) {
        return complaintRepository.searchComplaints(
                dto.getStatus(),
                dto.getKeyword(),
                dto.getClientId(),
                dto.getAgentAssuranceId(),
                dto.getAgentFinanceId(),
                dto.getDateDebut(),
                dto.getDateFin()
        ).stream().map(this::mapToDTO).toList();
    }

    // ================= KPI =================

    @Override
    public double calculateAverageProcessingTime() {
        List<Complaint> closed = complaintRepository.findByStatus("CLOSED");

        if (closed.isEmpty()) {
            return 0.0;
        }

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

        return count == 0 ? 0.0 : totalDays / (double) count;
    }

    @Override
    public double resolutionRate() {
        long total = complaintRepository.count();
        if (total == 0) {
            return 0.0;
        }
        long resolved = complaintRepository.countByStatus("APPROVED");
        return (resolved * 100.0) / total;
    }

    @Override
    public double rejectionRate() {
        long total = complaintRepository.count();
        if (total == 0) {
            return 0.0;
        }
        long rejected = complaintRepository.countByStatus("REJECTED");
        return (rejected * 100.0) / total;
    }

    @Override
    public String findTopAgent() {
        List<Complaint> allComplaints = complaintRepository.findAll();

        if (allComplaints.isEmpty()) {
            return "Aucun agent";
        }

        Map<String, Integer> agentCount = new HashMap<>();

        for (Complaint c : allComplaints) {
            if (c.getAgentAssurance() != null) {
                User agent = c.getAgentAssurance();
                String fullName = agent.getFirstName() + " " + agent.getLastName() + " (Assurance)";
                agentCount.put(fullName, agentCount.getOrDefault(fullName, 0) + 1);
            }
            if (c.getAgentFinance() != null) {
                User agent = c.getAgentFinance();
                String fullName = agent.getFirstName() + " " + agent.getLastName() + " (Finance)";
                agentCount.put(fullName, agentCount.getOrDefault(fullName, 0) + 1);
            }
        }

        if (agentCount.isEmpty()) {
            return "Aucun agent";
        }

        return agentCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Aucun agent");
    }

    @Override
    public Map<String, Object> getDashboardKpi() {
        Map<String, Object> dashboard = new HashMap<>();

        // KPIs principaux
        dashboard.put("averageProcessingTime", calculateAverageProcessingTime());
        dashboard.put("resolutionRate", resolutionRate());
        dashboard.put("rejectionRate", rejectionRate());
        dashboard.put("topAgent", findTopAgent());

        // Statistiques par statut
        Map<String, Object> statistics = new HashMap<>();
        long total = complaintRepository.count();
        long pending = complaintRepository.countByStatus("PENDING");
        long inProgress = complaintRepository.countByStatus("IN_PROGRESS");
        long approved = complaintRepository.countByStatus("APPROVED");
        long rejected = complaintRepository.countByStatus("REJECTED");
        long closed = complaintRepository.countByStatus("CLOSED");

        statistics.put("total", total);
        statistics.put("pending", pending);
        statistics.put("inProgress", inProgress);
        statistics.put("approved", approved);
        statistics.put("rejected", rejected);
        statistics.put("closed", closed);

        dashboard.put("statistics", statistics);

        return dashboard;
    }
}