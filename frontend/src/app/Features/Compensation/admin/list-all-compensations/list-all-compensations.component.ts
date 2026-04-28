import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { Compensation } from '../../../../shared';
import { CompensationService } from '../../services/compensation.service';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { Router } from '@angular/router';

@Component({
  selector: 'app-list-all-compensations',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule],
  templateUrl: './list-all-compensations.component.html',
  styleUrl: './list-all-compensations.component.css'
})
export class ListAllCompensationsComponent implements OnInit {
  
  compensations: Compensation[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  
  // Pour le filtrage
  filterStatus: string = '';
  filterRiskLevel: string = '';
  searchTerm: string = '';
  
  // Pour la modal de détails
  selectedCompensation: Compensation | null = null;
  showDetailsModal = false;
  scoringDetails: any = null;
  loadingScoring = false;
  
  // Pagination
  currentPage = 1;
  itemsPerPage = 10;
  
  // Niveaux de risque disponibles
  riskLevels = ['TRES_FAIBLE', 'FAIBLE', 'MODERE', 'ELEVE', 'TRES_ELEVE'];
  
  constructor(
    private compensationService: CompensationService,
    private toastr: ToastrService,
    private router: Router
  ) {}
  
  ngOnInit(): void {
    this.loadAllCompensations();
  }
  
  loadAllCompensations(): void {
    this.loading = true;
    this.errorMessage = '';
    
    this.compensationService.getAllCompensations().subscribe({
      next: (data) => {
        this.compensations = data;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur lors du chargement:', err);
        this.errorMessage = 'Impossible de charger les compensations. Veuillez réessayer.';
        this.loading = false;
      }
    });
  }
  
  // Obtenir le libellé du statut
  getStatusLabel(status: string): string {
    const statusMap: { [key: string]: string } = {
      'PENDING': 'En attente',
      'CALCULATED': 'Calculée',
      'PAID': 'Payée',
      'CANCELLED': 'Annulée'
    };
    return statusMap[status] || status;
  }
  
  // Obtenir la classe CSS pour le badge de statut
  getStatusClass(status: string): string {
    const classMap: { [key: string]: string } = {
      'PENDING': 'badge-warning',
      'CALCULATED': 'badge-info',
      'PAID': 'badge-success',
      'CANCELLED': 'badge-danger'
    };
    return classMap[status] || 'badge-secondary';
  }
  
  // Obtenir la classe CSS pour le niveau de risque
  getRiskClass(riskLevel: string): string {
    const classMap: { [key: string]: string } = {
      'TRES_FAIBLE': 'badge-success',
      'FAIBLE': 'badge-info',
      'MODERE': 'badge-warning',
      'ELEVE': 'badge-orange',
      'TRES_ELEVE': 'badge-danger'
    };
    return classMap[riskLevel] || 'badge-secondary';
  }
  
  // Obtenir le libellé du niveau de risque en français
  getRiskLabel(riskLevel: string): string {
    const labelMap: { [key: string]: string } = {
      'TRES_FAIBLE': 'Très faible',
      'FAIBLE': 'Faible',
      'MODERE': 'Modéré',
      'ELEVE': 'Élevé',
      'TRES_ELEVE': 'Très élevé'
    };
    return labelMap[riskLevel] || riskLevel;
  }
  
  // Obtenir l'icône du niveau de risque
  getRiskIcon(riskLevel: string): string {
    const iconMap: { [key: string]: string } = {
      'TRES_FAIBLE': '🟢',
      'FAIBLE': '🟢',
      'MODERE': '🟡',
      'ELEVE': '🟠',
      'TRES_ELEVE': '🔴'
    };
    return iconMap[riskLevel] || '⚪';
  }
  
  // Formater la date
  formatDate(date: Date | string): string {
    if (!date) return 'N/A';
    const d = new Date(date);
    return d.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
  
  // Formater le montant
  formatAmount(amount: number | undefined | null): string {
    if (amount === undefined || amount === null) return '0,00 DT';
    return new Intl.NumberFormat('fr-TN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount) + ' DT';
  }
  
  // Filtrer les compensations
  get filteredCompensations(): Compensation[] {
    let filtered = [...this.compensations];
    
    if (this.filterStatus) {
      filtered = filtered.filter(c => c.status === this.filterStatus);
    }
    
    if (this.filterRiskLevel) {
      filtered = filtered.filter(c => c.riskLevel === this.filterRiskLevel);
    }
    
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(c => 
        c.compensationId.toString().includes(term) ||
        c.message?.toLowerCase().includes(term) ||
        c.riskLevel?.toLowerCase().includes(term) ||
        c.claim?.claimId?.toString().includes(term)
      );
    }
    
    return filtered;
  }
  
  // Pagination
  get paginatedCompensations(): Compensation[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    return this.filteredCompensations.slice(start, end);
  }
  
  get totalPages(): number {
    return Math.ceil(this.filteredCompensations.length / this.itemsPerPage);
  }
  
  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }
  
  // Afficher les détails complets avec scoring (Modal)
  async viewDetailsModal(compensation: Compensation): Promise<void> {
    this.selectedCompensation = compensation;
    this.showDetailsModal = true;
    this.loadingScoring = true;
    
    try {
      this.scoringDetails = await this.compensationService.getCompensationWithScoring(compensation.compensationId).toPromise();
      this.loadingScoring = false;
    } catch (error) {
      console.error('Erreur chargement scoring:', error);
      this.loadingScoring = false;
      this.toastr.error('Impossible de charger les détails du scoring');
    }
  }
  
  // ✅ NOUVELLE MÉTHODE: Navigation vers la page admin details
  viewDetails(compensation: Compensation): void {
    this.router.navigate(['/backoffice/compensation', compensation.compensationId]);
  }
  
  closeModal(): void {
    this.showDetailsModal = false;
    this.selectedCompensation = null;
    this.scoringDetails = null;
  }
  
  // Marquer comme payée
  markAsPaid(compensation: Compensation): void {
    if (confirm(`Confirmer le paiement de la compensation #${compensation.compensationId} ?`)) {
      this.compensationService.markAsPaid(compensation.compensationId).subscribe({
        next: (response) => {
          this.successMessage = `Compensation #${compensation.compensationId} marquée comme payée`;
          this.toastr.success(`Compensation #${compensation.compensationId} marquée comme payée`);
          this.loadAllCompensations();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err: any) => {
          console.error('Erreur:', err);
          this.errorMessage = err.error?.error || 'Erreur lors du paiement';
          this.toastr.error(this.errorMessage);
          setTimeout(() => this.errorMessage = '', 3000);
        }
      });
    }
  }
  
  // Recalculer la compensation
  recalculate(compensation: Compensation): void {
    if (compensation.claim?.claimId) {
      if (confirm(`Recalculer la compensation #${compensation.compensationId} ?`)) {
        this.compensationService.recalculateCompensation(compensation.claim.claimId).subscribe({
          next: (response) => {
            this.successMessage = `Compensation #${compensation.compensationId} recalculée`;
            this.toastr.success(`Compensation #${compensation.compensationId} recalculée`);
            this.loadAllCompensations();
            setTimeout(() => this.successMessage = '', 3000);
          },
          error: (err: any) => {
            console.error('Erreur:', err);
            this.errorMessage = err.error?.error || 'Erreur lors du recalcul';
            this.toastr.error(this.errorMessage);
            setTimeout(() => this.errorMessage = '', 3000);
          }
        });
      }
    } else {
      this.toastr.warning('Impossible de recalculer: claim ID non trouvé');
    }
  }
  
  // Statistiques
  getTotalAmount(): number {
    return this.filteredCompensations.reduce((sum, c) => sum + (c.amount || 0), 0);
  }
  
  getAverageAmount(): number {
    if (this.filteredCompensations.length === 0) return 0;
    return this.getTotalAmount() / this.filteredCompensations.length;
  }
  
  getCountByStatus(status: string): number {
    return this.filteredCompensations.filter(c => c.status === status).length;
  }
  
  getCountByRiskLevel(riskLevel: string): number {
    return this.filteredCompensations.filter(c => c.riskLevel === riskLevel).length;
  }
}