package org.example.projet_pi.Controller;

import lombok.AllArgsConstructor;
import org.example.projet_pi.Service.IInsuranceProductService;
import org.example.projet_pi.entity.InsuranceProduct;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/products")
public class InsuranceProductController {

     IInsuranceProductService insuranceProductService;

    //crude

    @PostMapping("/addProduct")
    public InsuranceProduct addProduct(@RequestBody InsuranceProduct product) {
        InsuranceProduct product1 = insuranceProductService.addProduct(product);
        return product1;
    }

    @PutMapping("/updateProduct")
    public InsuranceProduct updateProduct(@RequestBody InsuranceProduct product) {
        InsuranceProduct product1 = insuranceProductService.updateProduct(product);
        return product1;
    }

    @DeleteMapping("/deleteProduct/{id}")
    public void deleteProduct(@PathVariable("id") Long id) {
        insuranceProductService.deleteProduct(id);
    }

    @GetMapping("/getProduct/{id}")
    public InsuranceProduct getProductById(@PathVariable("id") Long id) {
        InsuranceProduct product = insuranceProductService.getProductById(id);
        return product;
    }

    @GetMapping("/allProduct")
    public List<InsuranceProduct> getAllProducts() {
        List<InsuranceProduct> products = insuranceProductService.getAllProducts();
        return products;
    }
}
