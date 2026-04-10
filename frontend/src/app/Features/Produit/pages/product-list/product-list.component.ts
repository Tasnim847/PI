import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductApiService } from '../../services/product-api.service'; // Importer le composant
import { ProductCardComponent } from '../../components/product-card/product-card.component';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, ProductCardComponent], // Ajouter ProductCardComponent aux imports
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
      },
      error: (error) => {
        console.error('Erreur chargement produits:', error);
        this.isLoading = false;
      }
    });
  }

  // Pas besoin des méthodes getProductImageUrl, onImageError, addToCart ici
  // Car elles sont déjà gérées par ProductCardComponent
}