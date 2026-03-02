package org.example.projet_pi.Controller;

import org.example.projet_pi.Dto.ComplaintDTO;
import org.example.projet_pi.Dto.ComplaintSearchDTO;
import org.example.projet_pi.Service.IComplaintService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/complaints")
public class ComplaintController {

    private final IComplaintService complaintService;

    public ComplaintController(IComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    // ========== CRUD ==========

    @PostMapping("/addComplaint")
    public ResponseEntity<ComplaintDTO> addComplaint(@RequestBody ComplaintDTO complaintDTO) {
        ComplaintDTO saved = complaintService.addComplaint(complaintDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/updateComplaint/{id}")
    public ResponseEntity<ComplaintDTO> updateComplaint(@PathVariable Long id, @RequestBody ComplaintDTO complaintDTO) {
        ComplaintDTO updated = complaintService.updateComplaint(id, complaintDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/deleteComplaint/{id}")
    public ResponseEntity<Map<String, Object>> deleteComplaint(@PathVariable Long id) {
        complaintService.deleteComplaint(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Réclamation supprimée avec succès");
        response.put("id", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintDTO> getComplaintById(@PathVariable Long id) {
        ComplaintDTO dto = complaintService.getComplaintById(id);
        if (dto == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ComplaintDTO>> getAllComplaints() {
        List<ComplaintDTO> list = complaintService.getAllComplaints();
        return ResponseEntity.ok(list);
    }

    // ========== RECHERCHE ==========

    @PostMapping("/search")
    public ResponseEntity<List<ComplaintDTO>> searchComplaints(@RequestBody ComplaintSearchDTO dto) {
        List<ComplaintDTO> results = complaintService.searchComplaints(dto);
        return ResponseEntity.ok(results);
    }

    // ========== KPI ==========

    @GetMapping("/kpi/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardKpi() {
        Map<String, Object> dashboard = complaintService.getDashboardKpi();
        return ResponseEntity.ok(dashboard);
    }
}