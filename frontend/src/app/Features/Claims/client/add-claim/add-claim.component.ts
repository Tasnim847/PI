// add-claim.component.ts
import { Component, OnInit, OnDestroy, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ClaimsService } from '../../services/claims.service';
import { InsuranceContract, InsuranceProduct } from '../../../../shared';
import { ContractService } from '../../../Insurance/services/contract.service';
import { ProductService } from '../../../Produit/services/product.service';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

export enum ProductType {
  AUTO = 'AUTO',
  HEALTH = 'HEALTH',
  HOME = 'HOME',
  LIFE = 'LIFE',
  OTHER = 'OTHER'
}

@Component({
  selector: 'app-add-claim',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './add-claim.component.html',
  styleUrls: ['./add-claim.component.css']
})
export class AddClaimComponent implements OnInit, OnDestroy {
  @Output() claimCreated = new EventEmitter<void>();
  
  claimForm: FormGroup;
  contracts: InsuranceContract[] = [];
  loading = false;
  submitting = false;
  error = '';
  selectedProductType: string = '';
  selectedContract: InsuranceContract | null = null;
  selectedProduct: InsuranceProduct | null = null;
  ProductType = ProductType;
  private destroy$ = new Subject<void>();
  private isUpdatingValidators = false;

  selectedFiles: File[] = [];
  fileNames: string[] = [];

  constructor(
    private fb: FormBuilder,
    private claimsService: ClaimsService,
    private contractService: ContractService,
    private productService: ProductService,
    private router: Router
  ) {
    this.claimForm = this.fb.group({
      contractId: ['', Validators.required],
      claimedAmount: ['', [Validators.required, Validators.min(0.01)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      driverA: [''],
      driverB: [''],
      vehicleA: [''],
      vehicleB: [''],
      accidentLocation: [''],
      accidentDate: [''],
      patientName: [''],
      hospitalName: [''],
      doctorName: [''],
      medicalCost: [''],
      illnessType: [''],
      damageType: [''],
      address: [''],
      estimatedLoss: ['']
    });
  }

  ngOnInit(): void {
    this.loadContracts();
    
    this.claimForm.get('contractId')?.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(contractId => {
          if (!this.isUpdatingValidators && contractId) {
            const idAsNumber = Number(contractId);
            const contract = this.contracts.find(c => c.contractId === idAsNumber);
            if (contract && contract.productId) {
              return this.productService.getProductById(contract.productId);
            }
          }
          return [];
        })
      )
      .subscribe({
        next: (product) => {
          console.log('Produit chargé:', product);
          if (product) {
            this.selectedProduct = product;
            this.selectedProductType = product.productType;
            console.log('Type de produit récupéré:', this.selectedProductType);
            
            this.updateFormValidators();
            this.resetSpecificFields();
          } else {
            this.selectedProductType = '';
            this.selectedProduct = null;
            this.resetValidators();
          }
        },
        error: (err) => {
          console.error('Erreur chargement produit:', err);
          this.selectedProductType = '';
          this.selectedProduct = null;
          this.resetValidators();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadContracts(): void {
    this.loading = true;
    this.contractService.getMyContracts().subscribe({
      next: (data) => {
        console.log('Contrats reçus du backend:', data);
        this.contracts = data.filter(c => c.status === 'ACTIVE');
        console.log('Contrats actifs:', this.contracts);
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement contrats:', err);
        this.error = 'Erreur lors du chargement des contrats: ' + (err.error?.message || err.message);
        this.loading = false;
      }
    });
  }

  resetSpecificFields(): void {
    this.claimForm.patchValue({
      driverA: '',
      driverB: '',
      vehicleA: '',
      vehicleB: '',
      accidentLocation: '',
      accidentDate: '',
      patientName: '',
      hospitalName: '',
      doctorName: '',
      medicalCost: '',
      illnessType: '',
      damageType: '',
      address: '',
      estimatedLoss: ''
    });
  }

  updateFormValidators(): void {
    if (this.isUpdatingValidators) return;
    this.isUpdatingValidators = true;
    
    this.resetValidators();
    
    console.log('Mise à jour des validateurs pour le type:', this.selectedProductType);
    
    switch (this.selectedProductType) {
      case ProductType.AUTO:
      case 'AUTO':
        console.log('Application validateurs AUTO');
        this.claimForm.get('driverA')?.setValidators(Validators.required);
        this.claimForm.get('vehicleA')?.setValidators(Validators.required);
        this.claimForm.get('accidentLocation')?.setValidators(Validators.required);
        this.claimForm.get('accidentDate')?.setValidators(Validators.required);
        break;
      case ProductType.HEALTH:
      case 'HEALTH':
        console.log('Application validateurs HEALTH');
        this.claimForm.get('patientName')?.setValidators(Validators.required);
        this.claimForm.get('hospitalName')?.setValidators(Validators.required);
        this.claimForm.get('illnessType')?.setValidators(Validators.required);
        this.claimForm.get('medicalCost')?.setValidators([Validators.required, Validators.min(0.01)]);
        break;
      case ProductType.HOME:
      case 'HOME':
        console.log('Application validateurs HOME');
        this.claimForm.get('damageType')?.setValidators(Validators.required);
        this.claimForm.get('address')?.setValidators(Validators.required);
        this.claimForm.get('estimatedLoss')?.setValidators([Validators.required, Validators.min(0.01)]);
        break;
      default:
        console.log('Type de produit non reconnu:', this.selectedProductType);
    }
    
    Object.keys(this.claimForm.controls).forEach(key => {
      const control = this.claimForm.get(key);
      if (control) {
        control.updateValueAndValidity({ emitEvent: false });
      }
    });
    
    this.isUpdatingValidators = false;
  }

  resetValidators(): void {
    const fields = ['driverA', 'driverB', 'vehicleA', 'vehicleB', 'accidentLocation', 'accidentDate',
                    'patientName', 'hospitalName', 'doctorName', 'medicalCost', 'illnessType',
                    'damageType', 'address', 'estimatedLoss'];
    fields.forEach(field => {
      const control = this.claimForm.get(field);
      if (control) {
        control.clearValidators();
        control.updateValueAndValidity({ emitEvent: false });
      }
    });
  }

  onFileSelected(event: any): void {
    this.selectedFiles = Array.from(event.target.files);
    this.fileNames = this.selectedFiles.map(f => f.name);
  }

  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
    this.fileNames.splice(index, 1);
  }

  onSubmit(): void {
    if (this.claimForm.invalid) {
      Object.keys(this.claimForm.controls).forEach(key => {
        const control = this.claimForm.get(key);
        if (control?.invalid) {
          control.markAsTouched();
        }
      });
      this.error = 'Veuillez remplir tous les champs obligatoires';
      return;
    }

    if (this.selectedFiles.length === 0) {
      this.error = 'Veuillez joindre au moins un document justificatif';
      return;
    }

    if (!this.selectedProductType) {
      this.error = 'Veuillez sélectionner un contrat valide';
      return;
    }

    this.submitting = true;
    this.error = '';

    const claimDTO: any = {
      contractId: Number(this.claimForm.get('contractId')?.value),
      claimedAmount: Number(this.claimForm.get('claimedAmount')?.value),
      description: this.claimForm.get('description')?.value,
    };

    switch (this.selectedProductType) {
      case ProductType.AUTO:
      case 'AUTO':
        claimDTO.autoDetails = {
          driverA: this.claimForm.get('driverA')?.value,
          driverB: this.claimForm.get('driverB')?.value || '',
          vehicleA: this.claimForm.get('vehicleA')?.value,
          vehicleB: this.claimForm.get('vehicleB')?.value || '',
          accidentLocation: this.claimForm.get('accidentLocation')?.value,
          accidentDate: this.claimForm.get('accidentDate')?.value
        };
        break;

      case ProductType.HEALTH:
      case 'HEALTH':
        claimDTO.healthDetails = {
          patientName: this.claimForm.get('patientName')?.value,
          hospitalName: this.claimForm.get('hospitalName')?.value,
          doctorName: this.claimForm.get('doctorName')?.value || '',
          medicalCost: Number(this.claimForm.get('medicalCost')?.value),
          illnessType: this.claimForm.get('illnessType')?.value
        };
        break;

      case ProductType.HOME:
      case 'HOME':
        claimDTO.homeDetails = {
          damageType: this.claimForm.get('damageType')?.value,
          address: this.claimForm.get('address')?.value,
          estimatedLoss: Number(this.claimForm.get('estimatedLoss')?.value)
        };
        break;
    }

    console.log('DTO envoyé:', JSON.stringify(claimDTO, null, 2));

    this.claimsService.addClaim(claimDTO, this.selectedFiles).subscribe({
      next: (result) => {
        console.log('Succès:', result);
        this.submitting = false;
        this.claimCreated.emit(); // Émettre l'événement pour fermer le modal
        this.router.navigate(['/public/claims'], { 
          state: { success: '✅ Claim created successfully!' }
        });
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.error = err.error?.message || err.message || 'Error creating claim';
        this.submitting = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/public/claims']);
  }

}