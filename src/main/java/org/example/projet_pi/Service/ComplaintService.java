package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.ComplaintRepository;
import org.example.projet_pi.entity.Complaint;
import org.example.projet_pi.Dto.ComplaintSearchDTO;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
public class ComplaintService implements IComplaintService {

    private final ComplaintRepository complaintRepository;

    public ComplaintService(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    // ---------------- CRUD ----------------
    @Override
    public Complaint addComplaint(Complaint complaint) {
        return complaintRepository.save(complaint);
    }

    @Override
    public Complaint updateComplaint(Complaint complaint) {
        return complaintRepository.save(complaint);
    }

    @Override
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

}