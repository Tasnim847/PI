import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductApiService } from '../../services/product-api.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.css']
})
export class ProductListComponent implements OnInit {
  products: any[] = [];
  isLoading = true;

  constructor(private productApiService: ProductApiService) {}

  ngOnInit() {
    this.loadProducts();
  }

  loadProducts() {
    this.isLoading = true;
    this.productApiService.getActiveProducts().subscribe({
      next: (data) => {
        this.products = data;
        this.isLoading = false;
        console.log('Produits chargés:', this.products);
        // Vérifier les URLs des images
        if (this.products.length > 0) {
          console.log('URL de la première image:', this.products[0].displayImageUrl);
        }
      },
      error: (error) => {
        console.error('Erreur chargement produits:', error);
        this.isLoading = false;
      }
    });
  }

  getProductImageUrl(product: any): string {
    // Utiliser displayImageUrl qui est déjà formatée par le service
    return product.displayImageUrl || product.imageUrl || 'assets/images/produits-assurance.jpg';
  }

  onImageError(event: any) {
    event.target.src = 'assets/images/produits-assurance.jpg';
  }

  addToCart(product: any) {
    console.log('Ajout au panier:', product);
    // Implémentez votre logique de panier ici
  }
}