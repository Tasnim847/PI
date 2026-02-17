package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.CreditRepository;
import org.example.projet_pi.entity.Credit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CreditService implements ICreditService {

    private final CreditRepository creditRepository;

    //service


    public CreditService(CreditRepository creditRepository) {
        this.creditRepository = creditRepository;
    }

    @Override
    public Credit addCredit(Credit credit) {
        return creditRepository.save(credit);
    }

    @Override
    public Credit updateCredit(Credit credit) {
        return creditRepository.save(credit);
    }

    @Override
    public void deleteCredit(Long id) {
        creditRepository.deleteById(id);
    }

    @Override
    public Credit getCreditById(Long id) {
        return creditRepository.findById(id).orElse(null);
    }

    @Override
    public List<Credit> getAllCredits() {
        return creditRepository.findAll();
    }
}
