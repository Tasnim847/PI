import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClaimDTO } from '../../../../shared/dto/claim-dto.model';
import { ClaimStatus } from '../../../../shared';
import { ClaimsService } from '../../services/claims.service';

@Component({
  selector: 'app-list-all-claims',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './list-all-claims.component.html',
  styleUrls: ['./list-all-claims.component.css']
})
export class ListAllClaimsComponent implements OnInit {
  claims: ClaimDTO[] = [];
  filteredClaims: ClaimDTO[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  
  // Filters
  searchTerm = '';
  selectedStatus = '';
  statusOptions = Object.values(ClaimStatus);
  
  // Pagination - 8 items per page
  currentPage = 1;
  itemsPerPage = 8;
  
  // Modal
  selectedClaim: ClaimDTO | null = null;
  showDetailsModal = false;
  showCompensationModal = false;
  compensationDetails: any = null;
  compensationLoading = false;
  
  // Auto decision
  autoDecisionLoading = false;

  constructor(private claimsService: ClaimsService) {}

  ngOnInit(): void {
    this.loadClaims();
  }

  loadClaims(): void {
    this.loading = true;
    this.errorMessage = '';
    
    this.claimsService.getMyClaims().subscribe({
      next: (data) => {
        console.log('Claims loaded:', data);
        this.claims = data;
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading claims:', error);
        this.errorMessage = 'Unable to load claims. Please try again.';
        this.loading = false;
      }
    });
  }

  // Get client full name from the client object
  getClientFullName(claim: ClaimDTO): string {
    // Vérifie si claim.client existe
    if (claim.client) {
      const firstName = claim.client.firstName || '';
      const lastName = claim.client.lastName || '';
      const fullName = `${firstName} ${lastName}`.trim();
      return fullName || 'Client inconnu';
    }
    return 'Client inconnu';
  }

  // Get client name for search
  getClientNameForSearch(claim: ClaimDTO): string {
    if (claim.client) {
      return `${claim.client.firstName || ''} ${claim.client.lastName || ''}`.toLowerCase();
    }
    return '';
  }

  // Methods for statistics
  getDeclaredCount(): number {
    return this.claims.filter(c => c.status === 'DECLARED').length;
  }

  getInReviewCount(): number {
    return this.claims.filter(c => c.status === 'IN_REVIEW').length;
  }

  applyFilters(): void {
    let filtered = [...this.claims];
    
    // Filter by status
    if (this.selectedStatus) {
      filtered = filtered.filter(claim => claim.status === this.selectedStatus);
    }
    
    // Filter by search (ID, description, client name)
    if (this.searchTerm) {
      const search = this.searchTerm.toLowerCase();
      filtered = filtered.filter(claim => 
        claim.claimId?.toString().includes(search) ||
        claim.description?.toLowerCase().includes(search) ||
        this.getClientNameForSearch(claim).includes(search) ||
        claim.contractId?.toString().includes(search)
      );
    }
    
    // Sort by date descending
    filtered.sort((a, b) => {
      const dateA = a.claimDate ? new Date(a.claimDate).getTime() : 0;
      const dateB = b.claimDate ? new Date(b.claimDate).getTime() : 0;
      return dateB - dateA;
    });
    
    this.filteredClaims = filtered;
    this.currentPage = 1;
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedStatus = '';
    this.applyFilters();
  }

  get paginatedClaims(): ClaimDTO[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    return this.filteredClaims.slice(start, end);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredClaims.length / this.itemsPerPage);
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return 'status-unknown';
    
    switch (status) {
      case 'DECLARED': return 'status-declared';
      case 'IN_REVIEW': return 'status-review';
      case 'APPROVED': return 'status-approved';
      case 'REJECTED': return 'status-rejected';
      case 'COMPENSATED': return 'status-compensated';
      default: return 'status-unknown';
    }
  }

  getStatusLabel(status: string | undefined): string {
    if (!status) return 'Unknown';
    
    switch (status) {
      case 'DECLARED': return 'Declared';
      case 'IN_REVIEW': return 'In Review';
      case 'APPROVED': return 'Approved';
      case 'REJECTED': return 'Rejected';
      case 'COMPENSATED': return 'Compensated';
      default: return status;
    }
  }

  getStatusIcon(status: string | undefined): string {
    if (!status) return '❓';
    
    switch (status) {
      case 'DECLARED': return '📝';
      case 'IN_REVIEW': return '🔍';
      case 'APPROVED': return '✅';
      case 'REJECTED': return '❌';
      case 'COMPENSATED': return '💰';
      default: return '📋';
    }
  }

  viewDetails(claim: ClaimDTO): void {
    this.selectedClaim = claim;
    this.showDetailsModal = true;
  }

  closeModal(): void {
    this.showDetailsModal = false;
    this.selectedClaim = null;
  }

  viewCompensation(claimId: number | undefined): void {
    if (!claimId) return;
    
    this.showCompensationModal = true;
    this.compensationLoading = true;
    this.compensationDetails = null;
    
    this.claimsService.getCompensationDetails(claimId).subscribe({
      next: (data) => {
        this.compensationDetails = data;
        this.compensationLoading = false;
      },
      error: (error) => {
        console.error('Error calculating compensation:', error);
        this.compensationLoading = false;
        this.errorMessage = 'Unable to calculate compensation.';
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  closeCompensationModal(): void {
    this.showCompensationModal = false;
    this.compensationDetails = null;
  }

  autoDecision(claimId: number | undefined): void {
    if (!claimId) return;
    
    if (confirm('Do you want to execute automatic decision for this claim?')) {
      this.autoDecisionLoading = true;
      
      this.claimsService.autoDecision(claimId).subscribe({
        next: (updatedClaim) => {
          const index = this.claims.findIndex(c => c.claimId === updatedClaim.claimId);
          if (index !== -1) {
            this.claims[index] = updatedClaim;
          }
          this.applyFilters();
          this.autoDecisionLoading = false;
          this.successMessage = 'Automatic decision executed successfully!';
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (error) => {
          console.error('Error during automatic decision:', error);
          this.autoDecisionLoading = false;
          this.errorMessage = 'Error during automatic decision.';
          setTimeout(() => this.errorMessage = '', 3000);
        }
      });
    }
  }

  formatDate(date: Date | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatAmount(amount: number | undefined): string {
    if (amount === undefined || amount === null) return '0.00 DT';
    return amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' DT';
  }
}