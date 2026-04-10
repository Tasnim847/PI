import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../services/product.service';
import { InsuranceProduct, ProductStatus, ProductType } from '../../../../../shared';
import { ProductStatusLabels } from '../../../../../shared/enums/product-status.enum';
import { ProductTypeLabels } from '../../../../../shared/enums/product-type.enum';

@Component({
  selector: 'app-admin-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-product-list.component.html',
  styleUrls: ['./admin-product-list.component.css']
})
export class AdminProductListComponent implements OnInit {
  products: InsuranceProduct[] = [];
  isLoading = true;
  showAddModal = false;
  showEditModal = false;
  showDeleteModal = false;
  selectedProduct: InsuranceProduct | null = null;
  
  // Formulaire produit
  productFormData = {
    id: null as number | null,
    name: '',
    description: '',
    basePrice: 0,
    productType: '',
    otherProductType: '',
    status: 'ACTIVE'
  };
  
  selectedImage: File | null = null;
  imagePreview: string | null = null;

  // Enums et helpers
  productStatus = ProductStatus;
  productType = ProductType;
  productStatusLabels = ProductStatusLabels;
  productTypeLabels = ProductTypeLabels;

  constructor(private productService: ProductService) {}
  
  ngOnInit() {
    this.loadProducts();
  }
  
  loadProducts() {
    this.isLoading = true;
    this.productService.getAllProducts().subscribe({
      next: (products: InsuranceProduct[]) => {
        this.products = products;
        this.isLoading = false;
        console.log('Produits chargés:', products);
      },
      error: (error) => {
        console.error('Erreur chargement:', error);
        this.isLoading = false;
        alert('Erreur lors du chargement des produits');
      }
    });
  }

  onImageError(event: any) {
    console.warn('Erreur de chargement d\'image, utilisation de l\'image par défaut');
    event.target.src = 'assets/images/default-product.jpg';
    event.target.classList.add('image-error');
  }

  openAddModal() {
    this.resetForm();
    this.showAddModal = true;
  }

  openEditModal(product: InsuranceProduct) {
    this.selectedProduct = product;
    this.productFormData = {
      id: product.productId,
      name: product.name,
      description: product.description,
      basePrice: product.basePrice,
      productType: product.productType,
      otherProductType: product.otherType || '',
      status: product.status
    };
    
    if (product.displayImageUrl) {
      this.imagePreview = product.displayImageUrl;
    }
    
    this.showEditModal = true;
  }

  openDeleteModal(product: InsuranceProduct) {
    this.selectedProduct = product;
    this.showDeleteModal = true;
  }

  resetForm() {
    this.productFormData = {
      id: null,
      name: '',
      description: '',
      basePrice: 0,
      productType: '',
      otherProductType: '',
      status: 'ACTIVE'
    };
    this.selectedImage = null;
    this.imagePreview = null;
  }

  onImageSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedImage = file;
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.imagePreview = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage() {
    this.selectedImage = null;
    this.imagePreview = null;
  }

  addProduct() {
    const productData = {
      name: this.productFormData.name,
      description: this.productFormData.description,
      basePrice: this.productFormData.basePrice,
      productType: this.productFormData.productType,
      otherProductType: this.productFormData.productType === 'OTHER' ? this.productFormData.otherProductType : null,
      status: this.productFormData.status
    };
    
    console.log('Création produit - données:', productData);
    
    this.productService.addProduct(productData, this.selectedImage || undefined).subscribe({
      next: (response) => {
        console.log('Produit ajouté:', response);
        alert('Produit ajouté avec succès !');
        this.showAddModal = false;
        this.loadProducts();
        this.resetForm();
      },
      error: (error) => {
        console.error('Erreur ajout:', error);
        const message = typeof error.error === 'string' ? error.error : error.message;
        alert('Erreur: ' + message);
      }
    });
  }

  updateProduct() {
    const productData = {
      productId: this.productFormData.id,
      name: this.productFormData.name,
      description: this.productFormData.description,
      basePrice: this.productFormData.basePrice,
      productType: this.productFormData.productType,
      otherProductType: this.productFormData.productType === 'OTHER' ? this.productFormData.otherProductType : null,
      status: this.productFormData.status
    };

    console.log('Mise à jour produit - données:', productData);

    this.productService.updateProduct(productData, this.selectedImage || undefined).subscribe({
      next: (res: string) => {
        console.log("Réponse:", res);
        alert("Produit mis à jour avec succès");
        this.showEditModal = false;
        this.loadProducts();
        this.resetForm();
      },
      error: (err) => {
        console.error('Erreur mise à jour:', err);
        const message = typeof err.error === 'string' ? err.error : err.message;
        alert("Erreur: " + message);
      }
    });
  }

  deleteProduct() {
    if (this.selectedProduct) {
      this.productService.deleteProduct(this.selectedProduct.productId).subscribe({
        next: (response) => {
          console.log('Produit supprimé:', response);
          alert('Produit supprimé avec succès');
          this.showDeleteModal = false;
          this.loadProducts();
          this.selectedProduct = null;
        },
        error: (error) => {
          console.error('Erreur suppression:', error);
          const message = typeof error.error === 'string' ? error.error : error.message;
          alert('Erreur: ' + message);
        }
      });
    }
  }

  toggleProductStatus(product: InsuranceProduct) {
    if (product.status === ProductStatus.ACTIVE) {
      this.productService.deactivateProduct(product.productId).subscribe({
        next: (response) => {
          console.log('Produit désactivé:', response);
          alert('Produit désactivé avec succès');
          this.loadProducts();
        },
        error: (error) => {
          console.error('Erreur désactivation:', error);
          const message = typeof error.error === 'string' ? error.error : error.message;
          alert('Erreur: ' + message);
        }
      });
    } else if (product.status === ProductStatus.INACTIVE) {
      this.productService.activateProduct(product.productId).subscribe({
        next: (response) => {
          console.log('Produit activé:', response);
          alert('Produit activé avec succès');
          this.loadProducts();
        },
        error: (error) => {
          console.error('Erreur activation:', error);
          const message = typeof error.error === 'string' ? error.error : error.message;
          alert('Erreur: ' + message);
        }
      });
    } else if (product.status === ProductStatus.REFUSED) {
      alert('Ce produit a été refusé et ne peut pas être activé.');
    }
  }

  closeModals() {
    this.showAddModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.resetForm();
  }

  getProductTypeLabel(type: ProductType | string): string {
    return ProductTypeLabels[type as ProductType] || String(type);
  }

  getProductStatusLabel(status: ProductStatus | string): string {
    return ProductStatusLabels[status as ProductStatus] || String(status);
  }

  // Méthode utilitaire pour tronquer la description
  truncateDescription(description: string | undefined, maxLength: number = 80): string {
    if (!description) return '';
    if (description.length <= maxLength) return description;
    return description.substring(0, maxLength) + '...';
  }
}