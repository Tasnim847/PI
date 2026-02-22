package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.PaymentDTO;
import org.example.projet_pi.Mapper.PaymentMapper;
import org.example.projet_pi.Repository.InsuranceContractRepository;
import org.example.projet_pi.Repository.PaymentRepository;
import org.example.projet_pi.entity.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PaymentService implements IPaymentService {

    private final PaymentRepository paymentRepository;
    private final InsuranceContractRepository contractRepository;

    @Override
    public PaymentDTO addPayment(PaymentDTO dto) {

        InsuranceContract contract = contractRepository.findById(dto.getContractId())
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé"));

        if (contract.getStatus() == ContractStatus.COMPLETED)
            throw new RuntimeException("Contrat déjà payé");

        Payment payment = PaymentMapper.toEntity(dto);

        payment.setContract(contract);
        payment.setPaymentDate(new Date());
        payment.setStatus(PaymentStatus.PAID);

        // 🔥 LOGIQUE MÉTIER
        contract.applyPayment(payment.getAmount());

        payment = paymentRepository.save(payment);
        contractRepository.save(contract);

        return PaymentMapper.toDTO(payment);
    }

    @Override
    public PaymentDTO updatePayment(PaymentDTO dto) {
        throw new RuntimeException("Modification paiement interdite");
    }

    @Override
    public void deletePayment(Long id) {
        throw new RuntimeException("Suppression paiement interdite");
    }

    @Override
    public PaymentDTO getPaymentById(Long id) {
        return PaymentMapper.toDTO(
                paymentRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Paiement non trouvé"))
        );
    }

    @Override
    public List<PaymentDTO> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(PaymentMapper::toDTO)
                .collect(Collectors.toList());
    }
}