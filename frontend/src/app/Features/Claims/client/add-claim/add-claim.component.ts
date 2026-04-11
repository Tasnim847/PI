import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ClaimsService } from '../../services/claims.service';
import { InsuranceContract } from '../../../../shared';
import { ContractService } from '../../../Insurance/services/contract.service';
import { ClaimDTO } from '../../../../shared/dto/claim-dto.model';

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
  styleUrl: './add-claim.component.css'
})
export class AddClaimComponent implements OnInit {
  claimForm: FormGroup;
  contracts: InsuranceContract[] = [];
  loading = false;
  submitting = false;
  error = '';
  selectedProductType: string = '';
  ProductType = ProductType;

  // Pour l'upload de fichiers
  selectedFiles: File[] = [];
  fileNames: string[] = [];

  constructor(
    private fb: FormBuilder,
    private claimsService: ClaimsService,
    private contractService: ContractService,
    private router: Router
  ) {
    this.claimForm = this.fb.group({
      contractId: ['', Validators.required],
      claimedAmount: ['', [Validators.required, Validators.min(0.01)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      // Auto details
      driverA: [''],
      driverB: [''],
      vehicleA: [''],
      vehicleB: [''],
      accidentLocation: [''],
      accidentDate: [''],
      // Health details
      patientName: [''],
      hospitalName: [''],
      doctorName: [''],
      medicalCost: [''],
      illnessType: [''],
      // Home details
      damageType: [''],
      address: [''],
      estimatedLoss: ['']
    });
  }

  ngOnInit(): void {
    this.loadContracts();
    
    // Surveiller les changements de contrat
    this.claimForm.get('contractId')?.valueChanges.subscribe(contractId => {
      const contract = this.contracts.find(c => c.contractId === +contractId);
      if (contract) {
        this.selectedProductType = contract.product?.productType || '';
        this.updateFormValidators();
      }
    });
  }

  loadContracts(): void {
    this.loading = true;
    this.contractService.getMyContracts().subscribe({
      next: (data) => {
        this.contracts = data.filter(c => c.status === 'ACTIVE');
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des contrats: ' + (err.error?.message || err.message);
        this.loading = false;
      }
    });
  }

  updateFormValidators(): void {
    // Reset validators
    this.resetValidators();
    
    // Apply validators based on product type
    switch (this.selectedProductType) {
      case ProductType.AUTO:
        this.claimForm.get('driverA')?.setValidators(Validators.required);
        this.claimForm.get('vehicleA')?.setValidators(Validators.required);
        this.claimForm.get('accidentLocation')?.setValidators(Validators.required);
        this.claimForm.get('accidentDate')?.setValidators(Validators.required);
        break;
      case ProductType.HEALTH:
        this.claimForm.get('patientName')?.setValidators(Validators.required);
        this.claimForm.get('hospitalName')?.setValidators(Validators.required);
        this.claimForm.get('illnessType')?.setValidators(Validators.required);
        this.claimForm.get('medicalCost')?.setValidators([Validators.required, Validators.min(0.01)]);
        break;
      case ProductType.HOME:
        this.claimForm.get('damageType')?.setValidators(Validators.required);
        this.claimForm.get('address')?.setValidators(Validators.required);
        this.claimForm.get('estimatedLoss')?.setValidators([Validators.required, Validators.min(0.01)]);
        break;
    }
    
    // Update validators
    Object.keys(this.claimForm.controls).forEach(key => {
      this.claimForm.get(key)?.updateValueAndValidity();
    });
  }

  resetValidators(): void {
    const fields = ['driverA', 'driverB', 'vehicleA', 'vehicleB', 'accidentLocation', 'accidentDate',
                    'patientName', 'hospitalName', 'doctorName', 'medicalCost', 'illnessType',
                    'damageType', 'address', 'estimatedLoss'];
    fields.forEach(field => {
      this.claimForm.get(field)?.clearValidators();
      this.claimForm.get(field)?.updateValueAndValidity();
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
      return;
    }

    if (this.selectedFiles.length === 0) {
      this.error = 'Veuillez joindre au moins un document justificatif';
      return;
    }

    this.submitting = true;
    this.error = '';

    // Construire le DTO
    const claimDTO: ClaimDTO = {
      contractId: this.claimForm.get('contractId')?.value,
      claimedAmount: this.claimForm.get('claimedAmount')?.value,
      description: this.claimForm.get('description')?.value,
      documents: this.selectedFiles.map(file => ({
        name: file.name,
        type: file.type,
        filePath: URL.createObjectURL(file)
      }))
    };

    // Ajouter les détails spécifiques
    switch (this.selectedProductType) {
      case ProductType.AUTO:
        claimDTO.autoDetails = {
          id: 0,
          driverA: this.claimForm.get('driverA')?.value,
          driverB: this.claimForm.get('driverB')?.value || '',
          vehicleA: this.claimForm.get('vehicleA')?.value,
          vehicleB: this.claimForm.get('vehicleB')?.value || '',
          accidentLocation: this.claimForm.get('accidentLocation')?.value,
          accidentDate: this.claimForm.get('accidentDate')?.value
        };
        break;
      case ProductType.HEALTH:
        claimDTO.healthDetails = {
          id: 0,
          patientName: this.claimForm.get('patientName')?.value,
          hospitalName: this.claimForm.get('hospitalName')?.value,
          doctorName: this.claimForm.get('doctorName')?.value || '',
          medicalCost: this.claimForm.get('medicalCost')?.value,
          illnessType: this.claimForm.get('illnessType')?.value
        };
        break;
      case ProductType.HOME:
        claimDTO.homeDetails = {
          id: 0,
          damageType: this.claimForm.get('damageType')?.value,
          address: this.claimForm.get('address')?.value,
          estimatedLoss: this.claimForm.get('estimatedLoss')?.value
        };
        break;
    }

    this.claimsService.addClaim(claimDTO).subscribe({
      next: (result) => {
        this.submitting = false;
        this.router.navigate(['/public/claims'], { 
          state: { success: '✅ Réclamation créée avec succès !' }
        });
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors de la création de la réclamation';
        this.submitting = false;
      }
    });
  }
}