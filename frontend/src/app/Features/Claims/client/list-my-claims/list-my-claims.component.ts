// list-my-claims.component.ts
import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ClaimsService } from '../../services/claims.service';
import { ClaimDTO } from '../../../../shared/dto/claim-dto.model';
import { AddClaimComponent } from '../add-claim/add-claim.component';

@Component({
  selector: 'app-list-my-claims',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, AddClaimComponent],
  templateUrl: './list-my-claims.component.html',
  styleUrls: ['./list-my-claims.component.css']
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
  
  // Modal Add Claim
  showAddClaimModal = false;
  
  // Dropdown tracking
  activeDropdownClaimId: number | null = null;
  
  // Filters
  filterStatus: string = '';
  statusOptions = ['ALL', 'DECLARED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'COMPENSATED'];

  // Pagination
  currentPage = 1;
  itemsPerPage = 6;
  totalPages = 1;

  // Status configurations
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
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state?.['success']) {
      this.successMessage = navigation.extras.state['success'];
      setTimeout(() => this.successMessage = '', 5000);
    }
    this.loadClaims();
  }

  // Fermer le dropdown quand on clique ailleurs
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.dropdown')) {
      this.activeDropdownClaimId = null;
    }
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
        console.error('Error:', err);
        this.error = err.error?.message || err.message || 'Error loading claims';
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
        if (typeof data === 'string') {
          this.compensationDetails = data;
        } else {
          this.compensationDetails = JSON.stringify(data, null, 2);
        }
        this.showCompensationDetails = true;
      },
      error: (err) => {
        this.error = 'Error loading compensation details: ' + (err.error?.message || err.message);
      }
    });
  }

  closeModal(): void {
    this.selectedClaim = null;
    this.showCompensationDetails = false;
    this.compensationDetails = null;
  }

  // Add Claim Modal methods
  openAddClaimModal(): void {
    this.showAddClaimModal = true;
  }

  closeAddClaimModal(): void {
    this.showAddClaimModal = false;
  }

  onClaimCreated(): void {
    this.closeAddClaimModal();
    this.loadClaims();
    this.successMessage = 'Claim created successfully!';
    setTimeout(() => this.successMessage = '', 5000);
  }

  // Dropdown methods
  toggleDropdown(claimId: number | undefined, event: Event): void {
    event.stopPropagation();
    if (this.activeDropdownClaimId === claimId) {
      this.activeDropdownClaimId = null;
    } else {
      this.activeDropdownClaimId = claimId || null;
    }
  }

  closeDropdown(): void {
    this.activeDropdownClaimId = null;
  }

  // Statistics methods
  getApprovedCount(): number {
    return this.claims.filter(c => c.status === 'APPROVED').length;
  }

  getPendingCount(): number {
    return this.claims.filter(c => c.status === 'IN_REVIEW' || c.status === 'DECLARED').length;
  }

  getCompensatedCount(): number {
    return this.claims.filter(c => c.status === 'COMPENSATED').length;
  }

  getStatusClass(status: string): string {
    const statusMap: { [key: string]: string } = {
      'DECLARED': 'declared',
      'IN_REVIEW': 'in_review',
      'APPROVED': 'approved',
      'REJECTED': 'rejected',
      'COMPENSATED': 'compensated'
    };
    return `claim-status ${statusMap[status] || ''}`;
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

  editClaim(claimId: number | undefined, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/public/claims/edit', claimId]);
    this.closeDropdown();
  }

  deleteClaim(claimId: number | undefined, event: Event): void {
    event.stopPropagation();
    if (confirm('Are you sure you want to delete this claim? This action is irreversible.')) {
      this.claimsService.deleteClaim(claimId!).subscribe({
        next: () => {
          this.successMessage = 'Claim deleted successfully';
          this.loadClaims();
          setTimeout(() => this.successMessage = '', 3000);
          this.closeDropdown();
        },
        error: (err) => {
          this.error = err.error?.message || 'Error deleting claim';
        }
      });
    }
  }

  refresh(): void {
    this.loadClaims();
  }
}