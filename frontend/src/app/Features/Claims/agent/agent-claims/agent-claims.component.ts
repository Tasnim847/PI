// agent-claims.component.ts
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface Claim {
  claimId: number;
  claimedAmount: number;
  approvedAmount: number;
  description: string;
  status: string;
  claimDate: Date;
  message?: string;
  client?: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    telephone: string;
  };
}

@Component({
  selector: 'app-agent-claims',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './agent-claims.component.html',
  styleUrls: ['./agent-claims.component.css']
})
export class AgentClaimsComponent implements OnInit {
  claims: Claim[] = [];
  loading = false;
  error: string | null = null;
  approvedAmounts: { [key: number]: number } = {};
  rejectionReasons: { [key: number]: string } = {};
  selectedCompensation: any = null;
  searchTerm: string = '';
  selectedStatus: string = 'ALL';
  filteredClaims: Claim[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadClaims();
  }

  loadClaims() {
    this.loading = true;
    this.error = null;
    
    this.http.get<Claim[]>('http://localhost:8081/claims/allClaim')
      .subscribe({
        next: (data) => {
          this.claims = data;
          this.filteredClaims = data;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Erreur lors du chargement des réclamations';
          this.loading = false;
          console.error(err);
        }
      });
  }

  // Filter methods
  filterClaims() {
    let filtered = [...this.claims];
    
    // Filter by status
    if (this.selectedStatus !== 'ALL') {
      filtered = filtered.filter(c => c.status === this.selectedStatus);
    }
    
    // Filter by search term
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(c => 
        c.claimId.toString().includes(term) ||
        c.client?.firstName?.toLowerCase().includes(term) ||
        c.client?.lastName?.toLowerCase().includes(term) ||
        c.client?.email?.toLowerCase().includes(term) ||
        c.description?.toLowerCase().includes(term)
      );
    }
    
    this.filteredClaims = filtered;
  }

  setFilter(status: string) {
    this.selectedStatus = status;
    this.filterClaims();
  }

  clearFilters() {
    this.searchTerm = '';
    this.selectedStatus = 'ALL';
    this.filterClaims();
  }

  // Statistics methods
  getTotalClaims(): number {
    return this.claims.length;
  }

  getPendingCount(): number {
    return this.claims.filter(c => c.status === 'IN_REVIEW').length;
  }

  getApprovedCount(): number {
    return this.claims.filter(c => c.status === 'APPROVED').length;
  }

  getRejectedCount(): number {
    return this.claims.filter(c => c.status === 'REJECTED').length;
  }

  // Approve claim
  approveClaim(claimId: number) {
    const approvedAmount = this.approvedAmounts[claimId];
    
    if (!approvedAmount || approvedAmount <= 0) {
      alert('Veuillez entrer un montant valide à approuver');
      return;
    }

    const claim = this.claims.find(c => c.claimId === claimId);
    if (claim && approvedAmount > claim.claimedAmount) {
      alert(`Le montant approuvé ne peut pas dépasser ${claim.claimedAmount} DT`);
      return;
    }

    this.loading = true;
    this.http.post<Claim>(`http://localhost:8081/claims/approve/${claimId}`, null, {
      params: { approvedAmount: approvedAmount.toString() }
    }).subscribe({
      next: (updatedClaim) => {
        const index = this.claims.findIndex(c => c.claimId === claimId);
        if (index !== -1) {
          this.claims[index] = updatedClaim;
        }
        this.filterClaims();
        alert(`✅ Réclamation #${claimId} approuvée avec succès !`);
        delete this.approvedAmounts[claimId];
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        alert('Erreur: ' + (err.error?.message || err.message));
      }
    });
  }

  // Reject claim
  rejectClaim(claimId: number) {
    const reason = this.rejectionReasons[claimId];
    
    if (!reason || reason.trim() === '') {
      alert('Veuillez entrer une raison pour le rejet');
      return;
    }

    this.loading = true;
    this.http.post<Claim>(`http://localhost:8081/claims/reject/${claimId}`, null, {
      params: { reason: reason }
    }).subscribe({
      next: (updatedClaim) => {
        const index = this.claims.findIndex(c => c.claimId === claimId);
        if (index !== -1) {
          this.claims[index] = updatedClaim;
        }
        this.filterClaims();
        alert(`❌ Réclamation #${claimId} rejetée`);
        delete this.rejectionReasons[claimId];
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        alert('Erreur: ' + (err.error?.message || err.message));
      }
    });
  }

  // View compensation
  viewCompensation(claimId: number) {
    this.loading = true;
    this.http.get(`http://localhost:8081/claims/calculate-compensation/${claimId}`, {
      responseType: 'json'
    }).subscribe({
      next: (data) => {
        this.selectedCompensation = data;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        // Try text response as fallback
        this.http.get(`http://localhost:8081/claims/calculate-compensation/${claimId}/text`, {
          responseType: 'text'
        }).subscribe({
          next: (textData) => {
            this.selectedCompensation = textData;
          },
          error: (textErr) => {
            alert('Erreur lors du calcul de la compensation');
            console.error(textErr);
          }
        });
      }
    });
  }

  closeModal() {
    this.selectedCompensation = null;
  }

  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'IN_REVIEW': 'En révision',
      'APPROVED': 'Approuvé',
      'REJECTED': 'Rejeté',
      'COMPENSATED': 'Compensé',
      'PAID': 'Payé'
    };
    return labels[status] || status;
  }

  getStatusClass(status: string): string {
    const classes: { [key: string]: string } = {
      'IN_REVIEW': 'warning',
      'APPROVED': 'success',
      'REJECTED': 'danger',
      'COMPENSATED': 'info',
      'PAID': 'success'
    };
    return classes[status] || 'secondary';
  }
}