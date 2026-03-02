package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.ComplaintDTO;
import org.example.projet_pi.Dto.ComplaintSearchDTO;
import org.example.projet_pi.Repository.ClientRepository;
import org.example.projet_pi.Repository.ComplaintRepository;
import org.example.projet_pi.entity.Client;
import org.example.projet_pi.entity.Complaint;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComplaintService implements IComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ClientRepository clientRepository;

    public ComplaintService(ComplaintRepository complaintRepository, ClientRepository clientRepository) {
        this.complaintRepository = complaintRepository;
        this.clientRepository = clientRepository;
    }

    // 🔹 Conversion entity -> DTO
    private ComplaintDTO toDTO(Complaint complaint) {
        if (complaint == null) return null;
        ComplaintDTO dto = new ComplaintDTO();
        dto.setId(complaint.getId());
        dto.setStatus(complaint.getStatus());
        dto.setMessage(complaint.getMessage());
        dto.setClaimDate(complaint.getClaimDate() != null ? complaint.getClaimDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
        dto.setResolutionDate(complaint.getResolutionDate() != null ? complaint.getResolutionDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
        dto.setClientId(complaint.getClient() != null ? complaint.getClient().getId() : null);
        dto.setAgentAssuranceId(complaint.getAgentAssurance() != null ? complaint.getAgentAssurance().getId() : null);
        dto.setAgentFinanceId(complaint.getAgentFinance() != null ? complaint.getAgentFinance().getId() : null);
        return dto;
    }

    // 🔹 Conversion DTO -> entity
    private Complaint toEntity(ComplaintDTO dto) {
        Complaint complaint = new Complaint();
        complaint.setStatus(dto.getStatus());
        complaint.setMessage(dto.getMessage());
        // client, agents et dates seront remplis automatiquement
        return complaint;
    }

    // ---------------- CRUD ----------------

    @Override
    public ComplaintDTO addComplaint(ComplaintDTO dto) {
        Complaint complaint = toEntity(dto);

        // Récupère le client connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        complaint.setClient(client);
        complaint.setAgentAssurance(client.getAgentAssurance());
        complaint.setAgentFinance(client.getAgentFinance());
        complaint.setClaimDate(new Date());
        complaint.setStatus("PENDING");

        Complaint saved = complaintRepository.save(complaint);
        return toDTO(saved);
    }

    @Override
    public ComplaintDTO updateComplaint(Long id, ComplaintDTO dto) {
        Complaint existing = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint non trouvée"));

        if (dto.getMessage() != null) existing.setMessage(dto.getMessage());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());

        // On peut gérer les dates si nécessaires
        if (dto.getClaimDate() != null) existing.setClaimDate(java.util.Date.from(dto.getClaimDate().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        if (dto.getResolutionDate() != null) existing.setResolutionDate(java.util.Date.from(dto.getResolutionDate().atZone(java.time.ZoneId.systemDefault()).toInstant()));

        Complaint updated = complaintRepository.save(existing);
        return toDTO(updated);
    }

    @Override
    public void deleteComplaint(Long id) {
        complaintRepository.deleteById(id);
    }

    @Override
    public ComplaintDTO getComplaintById(Long id) {
        return complaintRepository.findById(id).map(this::toDTO).orElse(null);
    }

    @Override
    public List<ComplaintDTO> getAllComplaints() {
        return complaintRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ---------------- Recherche avancée ----------------

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
        ).stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ---------------- KPI ----------------

    @Override
    public double calculateAverageProcessingTime() {
        List<Complaint> closed = complaintRepository.findByStatus("CLOSED");
        if (closed.isEmpty()) return 0;

        long totalDays = 0;
        int count = 0;

        for (Complaint c : closed) {
            if (c.getClaimDate() != null && c.getResolutionDate() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                        c.getClaimDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                        c.getResolutionDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                );
                totalDays += days;
                count++;
            }
        }

        return count == 0 ? 0 : totalDays / (double) count;
    }

    @Override
    public double resolutionRate() {
        long total = complaintRepository.count();
        if (total == 0) return 0;
        long resolved = complaintRepository.countByStatus("APPROVED");
        return (resolved * 100.0) / total;
    }

    @Override
    public double rejectionRate() {
        long total = complaintRepository.count();
        if (total == 0) return 0;
        long rejected = complaintRepository.countByStatus("REJECTED");
        return (rejected * 100.0) / total;
    }

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
                .orElse("Aucun agent");
    }

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