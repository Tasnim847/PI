package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.InsuranceProductDTO;

import java.util.List;

public interface IInsuranceProductService {

    InsuranceProductDTO addProduct(InsuranceProductDTO dto);

    InsuranceProductDTO updateProduct(InsuranceProductDTO dto);

    void deleteProduct(Long id);

    InsuranceProductDTO getProductById(Long id);

    List<InsuranceProductDTO> getAllProducts();

    List<InsuranceProductDTO> getActiveProducts();

    InsuranceProductDTO changeProductStatus(Long productId, String statusStr);
}