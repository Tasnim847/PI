import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ContractService } from '../../services/contract.service';
import { AuthService } from '../../../../services/auth.service';
import { AddContractComponent } from '../client/add-contract/add-contract.component';
import { ToastrService } from 'ngx-toastr';
import { saveAs } from 'file-saver';

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
  searchTerm: string = '';
  isLoading = false;
  selectedContract: any = null;
  showRiskModal = false;
  showAddContractForm = false;
  showActivateModal = false;
  showRejectModal = false;
  rejectReason: string = '';
  showAddContractModal: boolean = false;
  
  // Informations utilisateur
  userRole: string = '';
  isAgent: boolean = false;
  isClient: boolean = false;
  isLoggedIn: boolean = false;

  // Statistiques
  stats = {
    total: 0,
    pending: 0,
    active: 0,
    completed: 0,
    cancelled: 0
  };

  statuses = ['ALL', 'PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED'];

  constructor(
    private contractService: ContractService,
    private authService: AuthService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.isLoggedIn = this.authService.isLoggedIn();
    this.userRole = this.authService.getRole() || '';
    this.isAgent = this.userRole === 'AGENT_ASSURANCE';
    this.isClient = this.userRole === 'CLIENT';
    
    if (this.isLoggedIn) {
      this.loadContracts();
    }
  }

  loadContracts(): void {
    this.isLoading = true;
    
    if (this.isClient) {
      // CLIENT: Utiliser getMyContracts() - ses propres contrats
      this.contractService.getMyContracts().subscribe({
        next: (contracts) => {
          this.contracts = contracts;
          this.calculateStats();
          this.filterContracts();
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Erreur:', err);
          this.toastr.error('Erreur lors du chargement de vos contrats');
          this.isLoading = false;
        }
      });
    } else if (this.isAgent) {
      // AGENT: Utiliser getAllContracts() - contrats des clients
      this.contractService.getAllContracts().subscribe({
        next: (contracts) => {
          this.contracts = contracts;
          this.calculateStats();
          this.filterContracts();
          this.isLoading = false;
          if (contracts.length > 0) {
            this.toastr.info(`Vous avez ${contracts.length} contrat(s) à gérer`);
          }
        },
        error: (err) => {
          console.error('Erreur:', err);
          this.toastr.error('Erreur lors du chargement des contrats');
          this.isLoading = false;
        }
      });
    } else {
      this.isLoading = false;
    }
  }

  calculateStats(): void {
    this.stats.total = this.contracts.length;
    this.stats.pending = this.contracts.filter(c => c.status === 'INACTIVE').length;
    this.stats.active = this.contracts.filter(c => c.status === 'ACTIVE').length;
    this.stats.completed = this.contracts.filter(c => c.status === 'COMPLETED').length;
    this.stats.cancelled = this.contracts.filter(c => c.status === 'CANCELLED').length;
  }

  filterContracts(): void {
    let filtered = this.contracts;
    
    if (this.selectedStatus !== 'ALL') {
      let statusFilter = this.selectedStatus;
      if (statusFilter === 'PENDING') statusFilter = 'INACTIVE';
      filtered = filtered.filter(c => c.status === statusFilter);
    }
    
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(c => 
        c.contractId.toString().includes(term) ||
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

  getStatusBadgeClass(status: string): string {
    const classes: {[key: string]: string} = {
      'ACTIVE': 'status-active',
      'INACTIVE': 'status-pending',
      'COMPLETED': 'status-completed',
      'CANCELLED': 'status-cancelled',
      'EXPIRED': 'status-expired'
    };
    return classes[status] || 'status-pending';
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

  // Actions pour l'agent
  openActivateModal(contract: any): void {
    if (contract.riskClaim?.riskLevel === 'HIGH') {
      this.toastr.error('Ce contrat ne peut pas être activé car son niveau de risque est ÉLEVÉ');
      return;
    }
    this.selectedContract = contract;
    this.showActivateModal = true;
  }

  confirmActivate(): void {
    if (this.selectedContract) {
      this.contractService.activateContract(this.selectedContract.contractId).subscribe({
        next: () => {
          this.toastr.success(`Contrat #${this.selectedContract.contractId} activé avec succès`);
          this.showActivateModal = false;
          this.loadContracts();
        },
        error: (err) => {
          this.toastr.error(err.error?.message || 'Erreur lors de l\'activation');
        }
      });
    }
  }

  openRejectModal(contract: any): void {
    this.selectedContract = contract;
    this.rejectReason = '';
    this.showRejectModal = true;
  }

  confirmReject(): void {
    if (this.selectedContract && this.rejectReason.trim()) {
      this.contractService.rejectContract(this.selectedContract.contractId, this.rejectReason).subscribe({
        next: () => {
          this.toastr.success(`Contrat #${this.selectedContract.contractId} rejeté`);
          this.showRejectModal = false;
          this.loadContracts();
        },
        error: (err) => {
          this.toastr.error(err.error?.message || 'Erreur lors du rejet');
        }
      });
    } else {
      this.toastr.warning('Veuillez saisir une raison de rejet');
    }
  }

  // Action pour le client
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

  getProgressPercentage(contract: any): number {
    if (!contract.premium || contract.premium === 0) return 0;
    return (contract.totalPaid / contract.premium) * 100;
  }

  closeModal(): void {
    this.showRiskModal = false;
    this.showActivateModal = false;
    this.showRejectModal = false;
    this.selectedContract = null;
    this.rejectReason = '';
  }

  // Méthodes pour le popup d'ajout de contrat (client uniquement)
  openAddContractModal(): void {
    if (!this.isLoggedIn) {
      this.toastr.warning('Veuillez vous connecter pour créer un contrat');
      return;
    }
    if (!this.isClient) {
      this.toastr.warning('Seuls les clients peuvent créer des contrats');
      return;
    }
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

  refresh(): void {
    this.loadContracts();
  }
}