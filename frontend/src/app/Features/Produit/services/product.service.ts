// services/product.service.ts
import { Injectable } from '@angular/core';
import { ProductApiService } from './product-api.service';
import { Observable } from 'rxjs';
import { InsuranceProduct } from '../../../shared';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(private api: ProductApiService) {}

  // ================= LIST =================
  getActiveProducts(): Observable<InsuranceProduct[]> {
    return this.api.getActiveProducts();
  }

  getAllProducts(): Observable<InsuranceProduct[]> {
    return this.api.getAllProducts();
  }

  // ================= CREATE =================
  addProduct(productData: any, image?: File): Observable<InsuranceProduct> {
    const formData = new FormData();
    // Ne pas inclure productId pour la création
    const cleanData = {
      name: productData.name,
      description: productData.description,
      basePrice: productData.basePrice,
      productType: productData.productType,
      otherProductType: productData.otherProductType,
      status: productData.status
    };
    formData.append('product', JSON.stringify(cleanData));

    if (image) {
      formData.append('image', image);
    }

    return this.api.addProduct(formData);
  }

  // ================= UPDATE =================
  updateProduct(productData: any, image?: File): Observable<string> {
    const formData = new FormData();
    // Utiliser productId (pas id)
    const cleanData = {
      productId: productData.productId,  // ← Changement clé
      name: productData.name,
      description: productData.description,
      basePrice: productData.basePrice,
      productType: productData.productType,
      otherProductType: productData.otherProductType,
      status: productData.status
    };
    formData.append('product', JSON.stringify(cleanData));

    if (image) {
      formData.append('image', image);
    }

    return this.api.updateProduct(formData);
  }

  // ================= DELETE =================
  deleteProduct(id: number): Observable<string> {
    return this.api.deleteProduct(id);
  }

  // ================= STATUS =================
  activateProduct(id: number): Observable<string> {
    return this.api.activateProduct(id);
  }

  deactivateProduct(id: number): Observable<string> {
    return this.api.deactivateProduct(id);
  }
}