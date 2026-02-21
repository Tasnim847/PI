package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.ICompensationService;
import org.example.projet_pi.Dto.CompensationDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/compensations")
public class CompensationController {

    private final ICompensationService compensationService;

    @PostMapping("/addComp")
    public CompensationDTO addCompensation(@RequestBody CompensationDTO dto) {
        return compensationService.addCompensation(dto);
    }

    @PutMapping("/updateComp")
    public CompensationDTO updateCompensation(@RequestBody CompensationDTO dto) {
        return compensationService.updateCompensation(dto);
    }

    @DeleteMapping("/deleteComp/{id}")
    public void deleteCompensation(@PathVariable Long id) {
        compensationService.deleteCompensation(id);
    }

    @GetMapping("/getComp/{id}")
    public CompensationDTO getCompensationById(@PathVariable Long id) {
        return compensationService.getCompensationById(id);
    }

    @GetMapping("/allComp")
    public List<CompensationDTO> getAllCompensations() {
        return compensationService.getAllCompensations();
    }
}