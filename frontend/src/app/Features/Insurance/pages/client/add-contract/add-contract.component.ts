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
    // Vérifier si l'utilisateur est connecté
    if (!this.authService.isLoggedIn()) {
      this.toastr.warning('Veuillez vous connecter pour créer un contrat');
      this.cancel.emit();
      return;
    }
    this.initForm();
    this.loadProducts();
  }

  initForm(): void {
    this.contractForm = this.fb.group({
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      premium: [{ value: 0, disabled: true }, [Validators.required, Validators.min(0)]],
      deductible: [{ value: 0, disabled: true }],
      coverageLimit: [{ value: 0, disabled: true }],
      paymentFrequency: ['MONTHLY', Validators.required],
      productId: ['', Validators.required]
    });

    // Recalculer la prime quand le produit change
    this.contractForm.get('productId')?.valueChanges.subscribe(productId => {
      this.updatePremiumFromProduct(productId);
    });
  }

  loadProducts(): void {
    this.productService.getAllProducts().subscribe({
      next: (products) => {
        this.products = products;
        console.log('Produits chargés:', products);
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.toastr.error('Erreur lors du chargement des produits');
      }
    });
  }

  updatePremiumFromProduct(productId: number): void {
    const product = this.products.find(p => (p as any).productId === productId || (p as any).id === productId);
    if (product) {
      const basePrice = (product as any).basePrice || (product as any).price || 0;
      const estimatedPremium = basePrice * 0.1;
      this.contractForm.patchValue({
        premium: estimatedPremium,
        deductible: estimatedPremium * 0.1,
        coverageLimit: basePrice * 10
      });
    }
  }

  onSubmit(): void {
    if (this.contractForm.invalid) {
      this.toastr.warning('Veuillez remplir tous les champs obligatoires');
      return;
    }

    // Vérifier à nouveau la connexion
    if (!this.authService.isLoggedIn()) {
      this.toastr.error('Veuillez vous reconnecter');
      this.cancel.emit();
      return;
    }

    this.isLoading = true;
    const formValue = this.contractForm.getRawValue();
    
    const contractData = {
      startDate: formValue.startDate,
      endDate: formValue.endDate,
      premium: formValue.premium,
      deductible: formValue.deductible,
      coverageLimit: formValue.coverageLimit,
      paymentFrequency: formValue.paymentFrequency,
      productId: formValue.productId
    };

    this.contractService.addContract(contractData).subscribe({
      next: (contract) => {
        this.isLoading = false;
        this.toastr.success('Contrat créé avec succès !');
        
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
            this.contractAdded.emit();
          }
        });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur:', err);
        if (err.status === 401) {
          this.toastr.error('Session expirée, veuillez vous reconnecter');
          this.authService.logout();
          this.cancel.emit();
        } else {
          this.toastr.error(err.error?.message || 'Erreur lors de la création du contrat');
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