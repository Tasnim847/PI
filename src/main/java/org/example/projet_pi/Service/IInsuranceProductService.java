package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.InsuranceProductDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IInsuranceProductService {

    InsuranceProductDTO addProduct(InsuranceProductDTO dto, MultipartFile imageFile);

    InsuranceProductDTO updateProduct(InsuranceProductDTO dto, MultipartFile imageFile);

    void deleteProduct(Long id);

    InsuranceProductDTO getProductById(Long id);

    List<InsuranceProductDTO> getAllProducts();

    List<InsuranceProductDTO> getActiveProducts();

    InsuranceProductDTO changeProductStatus(Long productId, String statusStr);

    InsuranceProductDTO assignImageToProduct(Long productId, MultipartFile imageFile);

    InsuranceProductDTO removeImageFromProduct(Long productId);
}