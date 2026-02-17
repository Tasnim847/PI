package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Repository.CompensationRepository;
import org.example.projet_pi.entity.Compensation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CompensationService implements ICompensationService {
    CompensationRepository compensationRepository;

    //service


    @Override
    public Compensation addCompensation(Compensation compensation) {
        return compensationRepository.save(compensation);
    }

    @Override
    public Compensation updateCompensation(Compensation compensation) {
        return compensationRepository.save(compensation);
    }

    @Override
    public void deleteCompensation(Long id) {
        compensationRepository.deleteById(id);
    }

    @Override
    public Compensation getCompensationById(Long id) {
        return compensationRepository.findById(id).get();
    }

    @Override
    public List<Compensation> getAllCompensations() {
        return compensationRepository.findAll();
    }
}
