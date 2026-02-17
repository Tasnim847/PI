package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.ICompensationService;
import org.example.projet_pi.entity.Compensation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/compensations")
public class CompensationController {

    ICompensationService compensationService;

    @PostMapping("/addCompensation")
    public Compensation addCompensation(@RequestBody Compensation compensation) {
        Compensation newCompensation = compensationService.addCompensation(compensation);
        return newCompensation;
    }

    @PutMapping("/updateCompensation")
    public Compensation updateCompensation(@RequestBody Compensation compensation) {
        Compensation newCompensation = compensationService.updateCompensation(compensation);
        return newCompensation;
    }

    @DeleteMapping("/deleteCompensation/{id}")
    public void deleteCompensation(@PathVariable("id") Long id) {
        compensationService.deleteCompensation(id);
    }

    @GetMapping("/getCompensation/{id}")
    public Compensation getCompensationById(@PathVariable("id") Long id) {
        Compensation compensation = compensationService.getCompensationById(id);
        return compensation;
    }

    @GetMapping("/allCompensation")
    public List<Compensation> getAllCompensations() {
        List<Compensation> compensations = compensationService.getAllCompensations();
        return compensations;
    }

}
