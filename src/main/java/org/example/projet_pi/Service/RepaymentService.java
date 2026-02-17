package org.example.projet_pi.Service;

import org.example.projet_pi.Repository.RepaymentRepository;
import org.example.projet_pi.Service.IRepaymentService;
import org.example.projet_pi.entity.Repayment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepaymentService implements IRepaymentService {

    private final RepaymentRepository repaymentRepository;

    public RepaymentService(RepaymentRepository repaymentRepository) {
        this.repaymentRepository = repaymentRepository;
    }

    @Override
    public Repayment addRepayment(Repayment repayment) {
        return repaymentRepository.save(repayment);
    }

    @Override
    public Repayment updateRepayment(Repayment repayment) {
        return repaymentRepository.save(repayment);
    }

    @Override
    public void deleteRepayment(Long id) {
        repaymentRepository.deleteById(id);
    }

    @Override
    public Repayment getRepaymentById(Long id) {
        return repaymentRepository.findById(id).orElse(null);
    }

    @Override
    public List<Repayment> getAllRepayments() {
        return repaymentRepository.findAll();
    }
}

