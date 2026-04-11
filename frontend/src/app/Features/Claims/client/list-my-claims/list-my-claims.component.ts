import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ClaimsService } from '../../services/claims.service';
import { ClaimDTO } from '../../../../shared/dto/claim-dto.model';

@Component({
  selector: 'app-list-my-claims',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './list-my-claims.component.html',
  styleUrl: './list-my-claims.component.css'
})
export class ListMyClaimsComponent implements OnInit {
  claims: ClaimDTO[] = [];
  filteredClaims: ClaimDTO[] = [];
  loading = false;
  error = '';
  successMessage = '';
  selectedClaim: ClaimDTO | null = null;
  showCompensationDetails = false;
  compensationDetails: any = null;
  
  // Filtres
  filterStatus: string = '';
  statusOptions = ['ALL', 'DECLARED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'COMPENSATED'];

  // Pagination
  currentPage = 1;
  itemsPerPage = 6;
  totalPages = 1;

  // Couleurs pour les statuts
  statusColors: { [key: string]: string } = {
    'DECLARED': 'badge bg-secondary',
    'IN_REVIEW': 'badge bg-warning text-dark',
    'APPROVED': 'badge bg-success',
    'REJECTED': 'badge bg-danger',
    'COMPENSATED': 'badge bg-info'
  };

  statusIcons: { [key: string]: string } = {
    'DECLARED': 'bi-file-text',
    'IN_REVIEW': 'bi-hourglass-split',
    'APPROVED': 'bi-check-circle',
    'REJECTED': 'bi-x-circle',
    'COMPENSATED': 'bi-cash-stack'
  };

  constructor(
    private claimsService: ClaimsService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Récupérer le message de succès depuis la navigation
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state?.['success']) {
      this.successMessage = navigation.extras.state['success'];
      setTimeout(() => this.successMessage = '', 5000);
    }
    this.loadClaims();
  }

  loadClaims(): void {
    this.loading = true;
    this.error = '';
    this.claimsService.getMyClaims().subscribe({
      next: (data) => {
        this.claims = data;
        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur détaillée:', err);
        this.error = err.error?.message || err.message || 'Erreur lors du chargement des réclamations';
        this.loading = false;
      }
    });
  }

  applyFilter(): void {
    if (this.filterStatus === '' || this.filterStatus === 'ALL') {
      this.filteredClaims = [...this.claims];
    } else {
      this.filteredClaims = this.claims.filter(claim => claim.status === this.filterStatus);
    }
    
    // Recalculer la pagination
    this.totalPages = Math.ceil(this.filteredClaims.length / this.itemsPerPage);
    this.currentPage = 1;
  }

  getPaginatedClaims(): ClaimDTO[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredClaims.slice(startIndex, endIndex);
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  viewClaimDetails(claim: ClaimDTO): void {
    this.selectedClaim = claim;
    this.showCompensationDetails = false;
    this.compensationDetails = null;
  }

  viewCompensation(claimId: number): void {
    this.claimsService.getCompensationDetails(claimId).subscribe({
      next: (data) => {
        // Si les données sont du texte, les afficher correctement
        if (typeof data === 'string') {
          this.compensationDetails = data;
        } else {
          this.compensationDetails = JSON.stringify(data, null, 2);
        }
        this.showCompensationDetails = true;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des détails de compensation: ' + (err.error?.message || err.message);
      }
    });
  }

  closeModal(): void {
    this.selectedClaim = null;
    this.showCompensationDetails = false;
    this.compensationDetails = null;
  }

  getStatusColor(status: string): string {
    return this.statusColors[status] || 'badge bg-secondary';
  }

  getStatusIcon(status: string): string {
    return this.statusIcons[status] || 'bi-file-text';
  }

  formatDate(date: Date | string): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatAmount(amount: number): string {
    if (!amount && amount !== 0) return '0,00 DT';
    return amount.toLocaleString('fr-FR', { 
      minimumFractionDigits: 2, 
      maximumFractionDigits: 2 
    }) + ' DT';
  }

  getProgressWidth(claimed: number, approved: number): number {
    if (!claimed || claimed === 0) return 0;
    const percentage = (approved / claimed) * 100;
    return Math.min(percentage, 100);
  }

  canEdit(claim: ClaimDTO): boolean {
    return claim.status === 'IN_REVIEW' || claim.status === 'DECLARED';
  }

  canDelete(claim: ClaimDTO): boolean {
    return claim.status === 'IN_REVIEW' || claim.status === 'DECLARED';
  }

  editClaim(claimId: number): void {
    this.router.navigate(['/public/claims/edit', claimId]);
  }

  deleteClaim(claimId: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette réclamation ? Cette action est irréversible.')) {
      this.claimsService.deleteClaim(claimId).subscribe({
        next: () => {
          this.successMessage = 'Réclamation supprimée avec succès';
          this.loadClaims();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          this.error = err.error?.message || 'Erreur lors de la suppression';
        }
      });
    }
  }

  refresh(): void {
    this.loadClaims();
  }
}