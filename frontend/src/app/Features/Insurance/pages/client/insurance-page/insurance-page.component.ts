// src/app/Features/Insurance/pages/insurance-page/insurance-page.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { saveAs } from 'file-saver';
import { AddContractComponent } from '../add-contract/add-contract.component';
import { ContractService } from '../../../services/contract.service';
import { AuthService } from '../../../../../services/auth.service';

@Component({
  selector: 'app-insurance-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AddContractComponent],
  templateUrl: './insurance-page.component.html',
  styleUrls: ['./insurance-page.component.css']
})
export class InsurancePageComponent implements OnInit {
  contracts: any[] = [];
  filteredContracts: any[] = [];
  selectedStatus: string = 'ALL';
  isLoading = false;
  selectedContract: any = null;
  showRiskModal = false;
  showAddContractForm = false;
  isLoggedIn = false;

  statuses = ['ALL', 'ACTIVE', 'INACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED'];

  constructor(
    private contractService: ContractService,
    private authService: AuthService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.checkAuthAndLoadContracts();
  }

  checkAuthAndLoadContracts(): void {
    this.isLoggedIn = this.authService.isLoggedIn();
    
    if (this.isLoggedIn) {
      this.loadContracts();
    } else {
      // Afficher un message invitant à se connecter
      this.toastr.info('Veuillez vous connecter pour voir vos contrats');
    }
  }

  loadContracts(): void {
    this.isLoading = true;
    this.contractService.getMyContracts().subscribe({
      next: (contracts) => {
        this.contracts = contracts;
        this.filterContracts();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur:', err);
        if (err.status === 401) {
          this.toastr.error('Session expirée, veuillez vous reconnecter');
          this.authService.logout();
        } else {
          this.toastr.error('Erreur lors du chargement des contrats');
        }
        this.isLoading = false;
      }
    });
  }

  filterContracts(): void {
    if (this.selectedStatus === 'ALL') {
      this.filteredContracts = this.contracts;
    } else {
      this.filteredContracts = this.contracts.filter(
        c => c.status === this.selectedStatus
      );
    }
  }

  onStatusChange(): void {
    this.filterContracts();
  }

  getStatusBadgeClass(status: string): string {
    const classes: {[key: string]: string} = {
      'ACTIVE': 'bg-success',
      'INACTIVE': 'bg-warning',
      'COMPLETED': 'bg-info',
      'CANCELLED': 'bg-danger',
      'EXPIRED': 'bg-secondary'
    };
    return classes[status] || 'bg-secondary';
  }

  getStatusLabel(status: string): string {
    const labels: {[key: string]: string} = {
      'ACTIVE': 'Actif',
      'INACTIVE': 'En attente',
      'COMPLETED': 'Terminé',
      'CANCELLED': 'Annulé',
      'EXPIRED': 'Expiré'
    };
    return labels[status] || status;
  }

  viewRisk(contract: any): void {
    this.selectedContract = contract;
    this.contractService.getContractRisk(contract.contractId).subscribe({
      next: (riskData) => {
        contract.riskClaim = riskData.riskEvaluation;
        this.showRiskModal = true;
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.toastr.error('Erreur lors du chargement de l\'évaluation du risque');
      }
    });
  }

  downloadPdf(contractId: number): void {
    this.contractService.downloadContractPdf(contractId).subscribe({
      next: (blob) => {
        saveAs(blob, `contrat_${contractId}.pdf`);
        this.toastr.success('PDF téléchargé avec succès');
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.toastr.error('Erreur lors du téléchargement du PDF');
      }
    });
  }

  cancelContract(contract: any): void {
    if (confirm('Êtes-vous sûr de vouloir annuler ce contrat ?')) {
      this.contractService.deleteContract(contract.contractId).subscribe({
        next: () => {
          this.toastr.success('Contrat annulé avec succès');
          this.loadContracts();
        },
        error: (err) => {
          console.error('Erreur:', err);
          this.toastr.error('Erreur lors de l\'annulation du contrat');
        }
      });
    }
  }

  formatDate(date: Date | string): string {
    if (!date) return 'N/A';
    const d = new Date(date);
    return d.toLocaleDateString('fr-FR');
  }

  closeModal(): void {
    this.showRiskModal = false;
    this.selectedContract = null;
  }

  toggleAddContractForm(): void {
    if (!this.isLoggedIn) {
      this.toastr.warning('Veuillez vous connecter pour créer un contrat');
      return;
    }
    this.showAddContractForm = !this.showAddContractForm;
  }

  onContractAdded(): void {
    this.showAddContractForm = false;
    this.loadContracts();
    this.toastr.success('Contrat créé avec succès !');
  }

  onCancelAddContract(): void {
    this.showAddContractForm = false;
  }
}