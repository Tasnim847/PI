package org.example.projet_pi.Service;

import org.example.projet_pi.entity.Complaint;
import org.example.projet_pi.Dto.ComplaintSearchDTO;
import java.util.List;
import java.util.Map;

public interface IComplaintService {

    // 🔹 CRUD
    Complaint addComplaint(Complaint complaint);
    Complaint updateComplaint(Complaint complaint);
    void deleteComplaint(Long id);
    Complaint getComplaintById(Long id);
    List<Complaint> getAllComplaints();

    // 🔹 Recherche avancée
    List<Complaint> searchComplaints(ComplaintSearchDTO dto);


}