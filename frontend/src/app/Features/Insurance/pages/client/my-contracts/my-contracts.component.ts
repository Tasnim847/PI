// my-contracts.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ContractService } from '../../../services/contract.service';
import { ToastrService } from 'ngx-toastr';
import { saveAs } from 'file-saver';
import { AddContractComponent } from '../add-contract/add-contract.component';

@Component({
  selector: 'app-my-contracts',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AddContractComponent],
  templateUrl: './my-contracts.component.html',
  styleUrls: ['./my-contracts.component.css']
})
export class MyContractsComponent implements OnInit {
  contracts: any[] = [];
  filteredContracts: any[] = [];
  paginatedContracts: any[] = [];
  selectedStatus: string = 'ALL';
  searchTerm: string = '';
  isLoading = false;
  selectedContract: any = null;
  showRiskModal = false;
  showDetailsModal = false;
  showAddContractModal = false;
  
  // Pagination - 6 cartes par page
  currentPage: number = 1;
  pageSize: number = 6;  // Changé de 9 à 6
  totalPages: number = 1;
  
  // Helper for Math in template
  Math = Math;
  
  // Statistiques
  stats = {
    total: 0,
    active: 0,
    pending: 0,
    completed: 0,
    cancelled: 0
  };

  constructor(
    private contractService: ContractService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadContracts();
  }

  loadContracts(): void {
    this.isLoading = true;
    this.contractService.getMyContracts().subscribe({
      next: (contracts) => {
        this.contracts = contracts;
        this.calculateStats();
        this.filterContracts();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.toastr.error('Erreur lors du chargement des contrats');
        this.isLoading = false;
      }
    });
  }

  calculateStats(): void {
    this.stats.total = this.contracts.length;
    this.stats.active = this.contracts.filter(c => c.status === 'ACTIVE').length;
    this.stats.pending = this.contracts.filter(c => c.status === 'INACTIVE').length;
    this.stats.completed = this.contracts.filter(c => c.status === 'COMPLETED').length;
    this.stats.cancelled = this.contracts.filter(c => c.status === 'CANCELLED').length;
  }

  filterContracts(): void {
    let filtered = this.contracts;
    
    // Filtre par statut
    if (this.selectedStatus !== 'ALL') {
      filtered = filtered.filter(c => c.status === this.selectedStatus);
    }
    
    // Filtre par recherche
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(c => 
        c.contractId.toString().includes(term) ||
        c.product?.name?.toLowerCase().includes(term) ||
        c.premium.toString().includes(term) ||
        c.status?.toLowerCase().includes(term) ||
        c.paymentFrequency?.toLowerCase().includes(term)
      );
    }
    
    this.filteredContracts = filtered;
    this.currentPage = 1;
    this.updatePagination();
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredContracts.length / this.pageSize);
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.paginatedContracts = this.filteredContracts.slice(start, end);
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePagination();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePagination();
    }
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePagination();
    }
  }

  onStatusChange(): void {
    this.filterContracts();
  }

  onSearch(): void {
    this.filterContracts();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.filterContracts();
  }

  applyQuickFilter(status: string): void {
    this.selectedStatus = status;
    this.filterContracts();
  }

  refresh(): void {
    this.loadContracts();
  }

  openAddContractModal(): void {
    this.showAddContractModal = true;
  }

  closeAddContractModal(): void {
    this.showAddContractModal = false;
  }

  onContractAdded(): void {
    this.closeAddContractModal();
    this.loadContracts();
    this.toastr.success('Contrat créé avec succès !');
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

  getProgressPercentage(contract: any): number {
    if (!contract.premium || contract.premium === 0) return 0;
    return (contract.totalPaid / contract.premium) * 100;
  }

  getDaysRemaining(endDate: string): number {
    const today = new Date();
    const end = new Date(endDate);
    const diffTime = end.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  }

  getContractDuration(contract: any): number {
    const start = new Date(contract.startDate);
    const end = new Date(contract.endDate);
    const diffTime = end.getTime() - start.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
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

  viewDetails(contract: any): void {
    this.selectedContract = contract;
    this.showDetailsModal = true;
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedContract = null;
  }

  downloadRiskReport(): void {
    if (this.selectedContract?.riskClaim) {
      const report = {
        contractId: this.selectedContract.contractId,
        evaluationDate: new Date(),
        riskLevel: this.selectedContract.riskClaim.riskLevel,
        riskScore: this.selectedContract.riskClaim.riskScore,
        evaluationNote: this.selectedContract.riskClaim.evaluationNote,
        recommendations: this.selectedContract.riskClaim.recommendations || []
      };
      
      const blob = new Blob([JSON.stringify(report, null, 2)], { type: 'application/json' });
      saveAs(blob, `rapport_risque_${this.selectedContract.contractId}.json`);
      this.toastr.success('Rapport téléchargé avec succès');
    }
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
    return d.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  closeModal(): void {
    this.showRiskModal = false;
    this.selectedContract = null;
  }
}