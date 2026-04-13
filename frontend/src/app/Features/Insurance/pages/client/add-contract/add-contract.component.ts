// src/app/Features/Insurance/pages/client/add-contract/add-contract.component.ts
import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ContractService } from '../../../services/contract.service';
import { ProductService } from '../../../../Produit/services/product.service';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../../../../services/auth.service';

@Component({
  selector: 'app-add-contract',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-contract.component.html',
  styleUrls: ['./add-contract.component.css']
})
export class AddContractComponent implements OnInit {
  @Output() contractAdded = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  contractForm!: FormGroup;
  products: any[] = [];
  paymentFrequencies = ['MONTHLY', 'SEMI_ANNUAL', 'ANNUAL'];
  isLoading = false;
  isLoadingProducts = false;
  riskEvaluation: any = null;

  constructor(
    private fb: FormBuilder,
    private contractService: ContractService,
    private productService: ProductService,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.toastr.warning('Veuillez vous connecter pour créer un contrat');
      this.cancel.emit();
      return;
    }
    this.initForm();
    this.loadActiveProducts();
  }

  initForm(): void {
    this.contractForm = this.fb.group({
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      paymentFrequency: ['MONTHLY', Validators.required],
      productId: ['', Validators.required]
    });
  }

  loadActiveProducts(): void {
    this.isLoadingProducts = true;
    
    this.productService.getActiveProducts().subscribe({
      next: (products) => {
        this.products = products.filter(p => p.status === 'ACTIVE');
        console.log('Produits actifs chargés:', this.products);
        this.isLoadingProducts = false;
        
        if (this.products.length === 0) {
          this.toastr.warning('Aucun produit actif disponible pour créer un contrat');
        }
      },
      error: (err) => {
        console.error('Erreur chargement produits:', err);
        this.loadAndFilterProducts();
      }
    });
  }

  loadAndFilterProducts(): void {
    this.productService.getAllProducts().subscribe({
      next: (products) => {
        this.products = products.filter(p => p.status === 'ACTIVE');
        console.log('Produits filtrés:', this.products);
        this.isLoadingProducts = false;
        
        if (this.products.length === 0) {
          this.toastr.warning('Aucun produit actif disponible');
        }
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.toastr.error('Erreur lors du chargement des produits');
        this.isLoadingProducts = false;
      }
    });
  }

  onSubmit(): void {
    if (this.contractForm.invalid) {
      this.toastr.warning('Veuillez remplir tous les champs obligatoires');
      return;
    }

    if (!this.authService.isLoggedIn()) {
      this.toastr.error('Veuillez vous reconnecter');
      this.cancel.emit();
      return;
    }

    this.isLoading = true;
    const formValue = this.contractForm.getRawValue();
    
    // Envoi uniquement des champs nécessaires
    const contractData = {
      startDate: formValue.startDate,
      endDate: formValue.endDate,
      paymentFrequency: formValue.paymentFrequency,
      productId: formValue.productId
    };

    console.log('📤 Envoi des données au backend:', contractData);

    this.contractService.addContract(contractData).subscribe({
      next: (contract) => {
        this.isLoading = false;
        this.toastr.success('✅ Contrat créé avec succès !');
        console.log('✅ Contrat reçu du backend:', contract);
        
        // Afficher les valeurs calculées par le backend
        this.toastr.info(`💰 Prime: ${contract.premium} DT | Franchise: ${contract.deductible} DT`);
        
        // Récupérer l'évaluation du risque
        this.contractService.getContractRisk(contract.contractId).subscribe({
          next: (riskData) => {
            this.riskEvaluation = riskData;
            if (!riskData.canBeActivated) {
              this.toastr.warning(riskData.recommendation);
            }
            this.contractAdded.emit();
          },
          error: (err) => {
            console.error('Erreur récupération risque:', err);
            this.contractAdded.emit();
          }
        });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('❌ Erreur création contrat:', err);
        
        if (err.status === 401) {
          this.toastr.error('Session expirée, veuillez vous reconnecter');
          this.authService.logout();
          this.cancel.emit();
        } else if (err.status === 403) {
          this.toastr.error('Vous n\'avez pas les droits pour créer un contrat');
        } else {
          const errorMsg = err.error?.message || err.message || 'Erreur lors de la création du contrat';
          this.toastr.error(errorMsg);
        }
      }
    });
  }

  onCancel(): void {
    this.cancel.emit();
  }

  getProductId(product: any): number {
    return product.productId || product.id || 0;
  }

  getProductName(product: any): string {
    return product.name || product.productName || 'Produit';
  }

  getProductBasePrice(product: any): number {
    return product.basePrice || product.price || 0;
  }
}