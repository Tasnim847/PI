// product-card.component.ts
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router'; // 👈 AJOUTER CET IMPORT

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, RouterModule], // 👈 AJOUTER RouterModule ICI
  templateUrl: './product-card.component.html',
  styleUrls: ['./product-card.component.css']
})
export class ProductCardComponent {
  @Input() product: any;

  @Output() addToCartEvent = new EventEmitter<any>();
  @Output() favoriteEvent = new EventEmitter<any>();
  @Output() compareEvent = new EventEmitter<any>();

  private readonly DEFAULT_IMAGE = 'assets/images/produits-assurance.jpg';
  private readonly BACKEND_IMAGE_BASE_URL = 'http://localhost:8083/products/images/';

  getImageUrl(): string {
    if (!this.product?.imageUrl) {
      return this.DEFAULT_IMAGE;
    }

    const filename = this.extractFilename(this.product.imageUrl);

    return filename
      ? `${this.BACKEND_IMAGE_BASE_URL}${filename}`
      : this.DEFAULT_IMAGE;
  }

  private extractFilename(path: string): string {
    if (!path) return '';

    return path
      .replace(/\\/g, '/')
      .split('/')
      .pop() || '';
  }

  onImageError(event: any) {
    event.target.src = this.DEFAULT_IMAGE;
  }

  formatPrice(price: number): string {
    if (!price && price !== 0) return '0 TND';

    return new Intl.NumberFormat('fr-TN', {
      style: 'currency',
      currency: 'TND',
      minimumFractionDigits: 0,
      maximumFractionDigits: 3
    }).format(price);
  }

  addToCart() {
    this.addToCartEvent.emit(this.product);
  }

  addToFavorites() {
    this.favoriteEvent.emit(this.product);
  }

  compare() {
    this.compareEvent.emit(this.product);
  }

  viewDetails() {
    console.log('Voir détails:', this.product);
  }
}