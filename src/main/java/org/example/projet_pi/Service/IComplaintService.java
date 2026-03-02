package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.ComplaintDTO;
import org.example.projet_pi.Dto.ComplaintSearchDTO;

import java.util.List;
import java.util.Map;

public interface IComplaintService {

    // 🔹 CRUD
    ComplaintDTO addComplaint(ComplaintDTO complaintDTO);
    ComplaintDTO updateComplaint(Long id, ComplaintDTO complaintDTO);
    void deleteComplaint(Long id);
    ComplaintDTO getComplaintById(Long id);
    List<ComplaintDTO> getAllComplaints();

    // 🔹 Recherche avancée
    List<ComplaintDTO> searchComplaints(ComplaintSearchDTO dto);

    // 🔥 KPI
    double calculateAverageProcessingTime();
    double resolutionRate();
    double rejectionRate();
    String findTopAgent();
    Map<String, Object> getDashboardKpi();
}