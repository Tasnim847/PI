package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Repository.ComplaintRepository;
import org.example.projet_pi.entity.Complaint;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplaintService implements IComplaintService{

    private final ComplaintRepository complaintRepository;

    //service

    public ComplaintService(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

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
complaintRepository.deleteById(id); }

    @Override
    public Complaint getComplaintById(Long id) {
        return complaintRepository.findById(id).get();
    }

    @Override
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }
}
