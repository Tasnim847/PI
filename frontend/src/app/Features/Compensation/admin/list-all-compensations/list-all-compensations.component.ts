import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { Compensation } from '../../../../shared';
import { CompensationService } from '../../services/compensation.service';
import { FormsModule } from '@angular/forms';

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
  searchTerm: string = '';
  
  // Pagination
  currentPage = 1;
  itemsPerPage = 10;
  
  constructor(private compensationService: CompensationService) {}
  
  ngOnInit(): void {
    this.loadAllCompensations();
  }
  
  loadAllCompensations(): void {
    this.loading = true;
    this.errorMessage = '';
    
    // Utilisation de l'endpoint existant pour admin
    this.compensationService.getAllCompensations().subscribe({
      next: (data) => {
        this.compensations = data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement:', error);
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
    return classMap[classMap[status] || 'badge-secondary'];
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
  formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-TN', {
      style: 'currency',
      currency: 'TND'
    }).format(amount);
  }
  
  // Filtrer les compensations
  get filteredCompensations(): Compensation[] {
    let filtered = [...this.compensations];
    
    if (this.filterStatus) {
      filtered = filtered.filter(c => c.status === this.filterStatus);
    }
    
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(c => 
        c.compensationId.toString().includes(term) ||
        c.message?.toLowerCase().includes(term) ||
        c.riskLevel?.toLowerCase().includes(term)
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
  
  // Actions admin
  viewDetails(compensation: Compensation): void {
    console.log('Voir détails:', compensation);
    // Implémenter la logique pour afficher les détails
  }
  
  markAsPaid(compensation: Compensation): void {
    if (confirm(`Confirmer le paiement de la compensation #${compensation.compensationId} ?`)) {
      this.compensationService.payCompensation(compensation.compensationId).subscribe({
        next: () => {
          this.successMessage = `Compensation #${compensation.compensationId} marquée comme payée`;
          this.loadAllCompensations();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (error) => {
          console.error('Erreur:', error);
          this.errorMessage = 'Erreur lors du paiement';
          setTimeout(() => this.errorMessage = '', 3000);
        }
      });
    }
  }
  
  recalculate(compensation: Compensation): void {
    if (compensation.claim?.claimId) {
      if (confirm(`Recalculer la compensation #${compensation.compensationId} ?`)) {
        // Note: L'endpoint /recalculate/{claimId} existe dans votre backend
        // Vous devrez peut-être ajouter cette méthode au service
        this.compensationService.recalculateCompensation(compensation.claim.claimId).subscribe({
          next: () => {
            this.successMessage = `Compensation #${compensation.compensationId} recalculée`;
            this.loadAllCompensations();
            setTimeout(() => this.successMessage = '', 3000);
          },
          error: (error) => {
            console.error('Erreur:', error);
            this.errorMessage = 'Erreur lors du recalcul';
            setTimeout(() => this.errorMessage = '', 3000);
          }
        });
      }
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
}