import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router'; // Ajouter Router
import { ToastrService } from 'ngx-toastr';
import { saveAs } from 'file-saver';
import { ContractService } from '../../../services/contract.service';

@Component({
  selector: 'app-agent-contracts',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './agent-contracts.component.html',
  styleUrls: ['./agent-contracts.component.css']
})
export class AgentContractsComponent implements OnInit {

  contracts: any[] = [];
  filteredContracts: any[] = [];

  selectedStatus = 'ALL';
  searchTerm = '';
  isLoading = false;

  selectedContract: any = null;

  showRiskModal = false;
  showActivateModal = false;
  showRejectModal = false;

  rejectReason = '';

  stats = {
    total: 0,
    pending: 0,
    active: 0,
    completed: 0,
    cancelled: 0
  };

  constructor(
    private contractService: ContractService,
    private toastr: ToastrService,
    private router: Router // Injecter Router
  ) {}

  ngOnInit(): void {
    this.loadContracts();
  }

  // ======================
  // LOAD
  // ======================
  loadContracts(): void {
    this.isLoading = true;

    this.contractService.getAllContracts().subscribe({
      next: (contracts: any[]) => {
        this.contracts = contracts || [];

        this.calculateStats();
        this.filterContracts();

        this.isLoading = false;

        this.toastr.success(`${this.contracts.length} contrats chargés`);
      },
      error: () => {
        this.toastr.error('Erreur chargement contrats');
        this.isLoading = false;
      }
    });
  }

  // ======================
  // FILTER
  // ======================
  filterContracts(): void {
    let filtered = [...this.contracts];

    if (this.selectedStatus !== 'ALL') {
      const status = this.selectedStatus === 'PENDING'
        ? 'INACTIVE'
        : this.selectedStatus;

      filtered = filtered.filter(c => c.status === status);
    }

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();

      filtered = filtered.filter(c =>
        c.contractId?.toString().includes(term) ||
        c.client?.firstName?.toLowerCase().includes(term) ||
        c.client?.lastName?.toLowerCase().includes(term) ||
        c.client?.email?.toLowerCase().includes(term) ||
        c.product?.name?.toLowerCase().includes(term)
      );
    }

    this.filteredContracts = filtered;
  }

  onStatusChange(): void {
    this.filterContracts();
  }

  onSearch(): void {
    this.filterContracts();
  }

  // ======================
  // STATS
  // ======================
  calculateStats(): void {
    this.stats.total = this.contracts.length;
    this.stats.pending = this.contracts.filter(c => c.status === 'INACTIVE').length;
    this.stats.active = this.contracts.filter(c => c.status === 'ACTIVE').length;
    this.stats.completed = this.contracts.filter(c => c.status === 'COMPLETED').length;
    this.stats.cancelled = this.contracts.filter(c => c.status === 'CANCELLED').length;
  }

  // ======================
  // STATUS
  // ======================
  getStatusLabel(status: string): string {
    return {
      ACTIVE: 'Actif',
      INACTIVE: 'En attente',
      COMPLETED: 'Terminé',
      CANCELLED: 'Annulé'
    }[status] || status;
  }

  getStatusClass(status: string): string {
    return {
      ACTIVE: 'active',
      INACTIVE: 'inactive',
      COMPLETED: 'completed',
      CANCELLED: 'cancelled'
    }[status] || 'inactive';
  }

  // ======================
  // ✔️ FIX MANQUANT (TON ERREUR)
  // ======================
  getPaymentFrequencyLabel(freq: string): string {
    return {
      MONTHLY: 'Mensuel',
      SEMI_ANNUAL: 'Semestriel',
      ANNUAL: 'Annuel'
    }[freq] || freq || 'N/A';
  }

  // ======================
  // UTILITIES
  // ======================
  formatDate(date: any): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('fr-FR');
  }

  getDaysRemaining(endDate: any): number {
    if (!endDate) return -1;
    return Math.ceil((new Date(endDate).getTime() - Date.now()) / 86400000);
  }

  getProgressPercentage(contract: any): number {
    if (!contract.premium) return 0;
    return Math.min((contract.totalPaid / contract.premium) * 100, 100);
  }

  // ======================
  // ACTIONS
  // ======================
  viewRisk(contract: any): void {
    // Navigation vers la page dashboard avec l'ID du contrat
    this.router.navigate(['public/agent/dashboard'], { 
      queryParams: { contractId: contract.contractId }
    });
  }

  downloadPdf(id: number): void {
    this.contractService.downloadContractPdf(id).subscribe({
      next: (blob: Blob) => {
        saveAs(blob, `contract_${id}.pdf`);
      },
      error: () => this.toastr.error('Erreur PDF')
    });
  }

  openActivateModal(contract: any): void {
    this.selectedContract = contract;
    this.showActivateModal = true;
  }

  confirmActivate(): void {
    this.contractService.activateContract(this.selectedContract.contractId).subscribe({
      next: () => {
        this.toastr.success('Contrat activé');
        this.loadContracts();
        this.showActivateModal = false;
      },
      error: () => this.toastr.error('Erreur activation')
    });
  }

  openRejectModal(contract: any): void {
    this.selectedContract = contract;
    this.rejectReason = '';
    this.showRejectModal = true;
  }

  confirmReject(): void {
    this.contractService.rejectContract(
      this.selectedContract.contractId,
      this.rejectReason
    ).subscribe({
      next: () => {
        this.toastr.success('Contrat rejeté');
        this.loadContracts();
        this.showRejectModal = false;
      },
      error: () => this.toastr.error('Erreur rejet')
    });
  }

  refresh(): void {
    this.loadContracts();
  }

  closeModal(): void {
    this.showRiskModal = false;
    this.showActivateModal = false;
    this.showRejectModal = false;
    this.selectedContract = null;
  }
}