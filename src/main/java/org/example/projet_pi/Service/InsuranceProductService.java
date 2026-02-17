package org.example.projet_pi.Service;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Repository.InsuranceProductRepository;
import org.example.projet_pi.entity.InsuranceProduct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class InsuranceProductService implements IInsuranceProductService{

    //service
    InsuranceProductRepository insuranceProductRepository;

    @Override
    public InsuranceProduct addProduct(InsuranceProduct product) {
        return insuranceProductRepository.save(product);
    }

    @Override
    public InsuranceProduct updateProduct(InsuranceProduct product) {
        return insuranceProductRepository.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        insuranceProductRepository.deleteById(id);
    }

    @Override
    public InsuranceProduct getProductById(Long id) {
        return insuranceProductRepository.getReferenceById(id);
    }

    @Override
    public List<InsuranceProduct> getAllProducts() {
        return insuranceProductRepository.findAll();
    }
}
