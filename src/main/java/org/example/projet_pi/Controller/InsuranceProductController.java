package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Dto.InsuranceProductDTO;
import org.example.projet_pi.Service.IInsuranceProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/products")
public class InsuranceProductController {

    private final IInsuranceProductService insuranceProductService;

    @PostMapping("/addProduct")
    public InsuranceProductDTO addProduct(@RequestBody InsuranceProductDTO dto) {
        return insuranceProductService.addProduct(dto);
    }

    @PutMapping("/updateProduct")
    public InsuranceProductDTO updateProduct(@RequestBody InsuranceProductDTO dto) {
        return insuranceProductService.updateProduct(dto);
    }

    @DeleteMapping("/deleteProduct/{id}")
    public void deleteProduct(@PathVariable Long id) {
        insuranceProductService.deleteProduct(id);
    }

    @GetMapping("/getProduct/{id}")
    public InsuranceProductDTO getProductById(@PathVariable Long id) {
        return insuranceProductService.getProductById(id);
    }

    @GetMapping("/allProduct")
    public List<InsuranceProductDTO> getAllProducts() {
        return insuranceProductService.getAllProducts();
    }
}