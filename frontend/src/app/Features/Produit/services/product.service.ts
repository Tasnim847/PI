import { Injectable } from '@angular/core';
import { ProductApiService } from './product-api.service';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(private productApiService: ProductApiService) {}

  getActiveProducts() {
    return this.productApiService.getActiveProducts().pipe(
      map(products => {
        // Assurez-vous que chaque produit a toutes les propriétés nécessaires
        return products.map(product => ({
          ...product,
          // Pour compatibilité avec le template existant
          price: product.basePrice,
          oldPrice: product.oldPrice || null,
          rating: product.rating || 4.5,
          reviews: product.reviews || 0,
          isNew: product.isNew || false
        }));
      })
    );
  }
}