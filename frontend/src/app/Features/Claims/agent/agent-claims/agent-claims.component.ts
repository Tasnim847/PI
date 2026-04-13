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
  selectedCompensation: string | null = null;

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
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Erreur lors du chargement des réclamations';
          this.loading = false;
          console.error(err);
        }
      });
  }

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

    this.http.post<Claim>(`http://localhost:8081/claims/approve/${claimId}`, null, {
      params: { approvedAmount: approvedAmount.toString() }
    }).subscribe({
      next: (updatedClaim) => {
        const index = this.claims.findIndex(c => c.claimId === claimId);
        if (index !== -1) {
          this.claims[index] = updatedClaim;
        }
        alert(`✅ Réclamation #${claimId} approuvée avec succès !`);
        delete this.approvedAmounts[claimId];
        this.loadClaims();
      },
      error: (err) => {
        alert('Erreur: ' + (err.error?.message || err.message));
      }
    });
  }

  rejectClaim(claimId: number) {
    const reason = this.rejectionReasons[claimId];
    
    if (!reason || reason.trim() === '') {
      alert('Veuillez entrer une raison pour le rejet');
      return;
    }

    this.http.post<Claim>(`http://localhost:8081/claims/reject/${claimId}`, null, {
      params: { reason: reason }
    }).subscribe({
      next: (updatedClaim) => {
        const index = this.claims.findIndex(c => c.claimId === claimId);
        if (index !== -1) {
          this.claims[index] = updatedClaim;
        }
        alert(`❌ Réclamation #${claimId} rejetée`);
        delete this.rejectionReasons[claimId];
      },
      error: (err) => {
        alert('Erreur: ' + (err.error?.message || err.message));
      }
    });
  }

  viewCompensation(claimId: number) {
    this.http.get(`http://localhost:8081/claims/calculate-compensation/${claimId}/text`, {
      responseType: 'text'
    }).subscribe({
      next: (data) => {
        this.selectedCompensation = data;
      },
      error: (err) => {
        alert('Erreur lors du calcul de la compensation');
      }
    });
  }

  closeModal() {
    this.selectedCompensation = null;
  }

  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'IN_REVIEW': '📝 En révision',
      'APPROVED': '✅ Approuvé',
      'REJECTED': '❌ Rejeté',
      'COMPENSATED': '💰 Compensé'
    };
    return labels[status] || status;
  }
}