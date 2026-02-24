package org.example.projet_pi.Controller;

import org.example.projet_pi.Service.IComplaintService;
import org.example.projet_pi.entity.Complaint;
import org.example.projet_pi.Dto.ComplaintSearchDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/complaints")
public class ComplaintController {

    @Autowired
    private IComplaintService complaintService;

    // 🔹 Ajouter une réclamation
    @PostMapping("/add")
    public Complaint addComplaint(@RequestBody Complaint complaint) {
        return complaintService.addComplaint(complaint);
    }

    // 🔹 Modifier une réclamation
    @PutMapping("/update")
    public Complaint updateComplaint(@RequestBody Complaint complaint) {
        return complaintService.updateComplaint(complaint);
    }

    // 🔹 Supprimer une réclamation
    @DeleteMapping("/delete/{id}")
    public void deleteComplaint(@PathVariable Long id) {
        complaintService.deleteComplaint(id);
    }

    // 🔹 Récupérer une réclamation par ID
    @GetMapping("/{id}")
    public Complaint getComplaintById(@PathVariable Long id) {
        return complaintService.getComplaintById(id);
    }

    // 🔹 Récupérer toutes les réclamations
    @GetMapping("/all")
    public List<Complaint> getAllComplaints() {
        return complaintService.getAllComplaints();
    }

    // 🔹 Recherche avancée par critères
    @PostMapping("/search")
    public List<Complaint> search(@RequestBody ComplaintSearchDTO dto) {
        return complaintService.searchComplaints(dto);
    }

}