// contract-list.component.ts (admin)
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { ContractService } from '../../../services/contract.service';

@Component({
  selector: 'app-contract-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './contract-list.component.html',
  styleUrls: ['./contract-list.component.css']
})
export class ContractListComponent implements OnInit {
  contracts: any[] = [];
  filteredContracts: any[] = [];
  selectedStatus: string = 'ALL';
  searchTerm: string = '';
  isLoading = false;
  selectedContract: any = null;
  showRiskModal = false;
  showSimulateModal = false;
  simulateMonths: number = 1;

  statuses = ['ALL', 'ACTIVE', 'INACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED'];

  constructor(
    private contractService: ContractService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadContracts();
  }

  loadContracts(): void {
    this.isLoading = true;
    this.contractService.getAllContracts().subscribe({
      next: (contracts) => {
        this.contracts = contracts;
        this.filterContracts();
        this.isLoading = false;
      },
      error: (err) => {
        this.toastr.error('Erreur lors du chargement des contrats');
        this.isLoading = false;
      }
    });
  }

  filterContracts(): void {
    this.filteredContracts = this.contracts.filter(contract => {
      const matchStatus = this.selectedStatus === 'ALL' || contract.status === this.selectedStatus;
      const matchSearch = !this.searchTerm || 
        contract.contractId.toString().includes(this.searchTerm) ||
        contract.client?.email?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        contract.product?.name?.toLowerCase().includes(this.searchTerm.toLowerCase());
      return matchStatus && matchSearch;
    });
  }

  getStatusBadgeClass(status: string): string {
    const classes: any = {
      'ACTIVE': 'bg-success',
      'INACTIVE': 'bg-warning',
      'COMPLETED': 'bg-info',
      'CANCELLED': 'bg-danger',
      'EXPIRED': 'bg-secondary'
    };
    return classes[status] || 'bg-secondary';
  }

  getStatusLabel(status: string): string {
    const labels: any = {
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
        this.toastr.error('Erreur lors du chargement du risque');
      }
    });
  }

  activateContract(contract: any): void {
    if (confirm(`Activer le contrat #${contract.contractId} ?`)) {
      this.contractService.activateContract(contract.contractId).subscribe({
        next: () => {
          this.toastr.success('Contrat activé avec succès');
          this.loadContracts();
        },
        error: (err) => {
          this.toastr.error(err.error?.message || 'Erreur lors de l\'activation');
        }
      });
    }
  }

  rejectContract(contract: any): void {
    const reason = prompt('Raison du rejet :');
    if (reason) {
      this.contractService.rejectContract(contract.contractId, reason).subscribe({
        next: () => {
          this.toastr.success('Contrat rejeté');
          this.loadContracts();
        },
        error: (err) => {
          this.toastr.error('Erreur lors du rejet');
        }
      });
    }
  }

  simulateLatePayment(contract: any): void {
    this.selectedContract = contract;
    this.showSimulateModal = true;
  }

  confirmSimulate(): void {
    if (this.selectedContract && this.simulateMonths > 0) {
      this.contractService.simulateLatePayments(this.selectedContract.contractId, this.simulateMonths).subscribe({
        next: (response) => {
          this.toastr.success(`${this.simulateMonths} mois de retard simulés`);
          this.showSimulateModal = false;
          this.loadContracts();
        },
        error: (err) => {
          this.toastr.error('Erreur lors de la simulation');
        }
      });
    }
  }

  runSystemCheck(): void {
    this.contractService.checkLatePayments().subscribe({
      next: () => {
        this.toastr.success('Vérification des retards effectuée');
        this.loadContracts();
      },
      error: () => this.toastr.error('Erreur lors de la vérification')
    });
  }

  formatDate(date: Date | string): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('fr-FR');
  }

  closeModal(): void {
    this.showRiskModal = false;
    this.showSimulateModal = false;
    this.selectedContract = null;
    this.simulateMonths = 1;
  }
}