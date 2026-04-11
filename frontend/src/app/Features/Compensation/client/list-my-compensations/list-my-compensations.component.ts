import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CompensationService } from '../../services/compensation.service';
import { Compensation } from '../../../../shared';

@Component({
  selector: 'app-list-my-compensations',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './list-my-compensations.component.html',
  styleUrl: './list-my-compensations.component.css'
})
export class ListMyCompensationsComponent implements OnInit {
  compensations: Compensation[] = [];
  loading = false;
  error = '';
  selectedCompensation: any = null;
  showDetails = false;
  paymentStatus: any = null;

  statusColors: { [key: string]: string } = {
    'PENDING': 'badge bg-warning',
    'CALCULATED': 'badge bg-info',
    'PAID': 'badge bg-success',
    'CANCELLED': 'badge bg-danger'
  };

  riskLevelColors: { [key: string]: string } = {
    'TRES_FAIBLE': 'text-success',
    'FAIBLE': 'text-info',
    'MOYEN': 'text-warning',
    'ELEVE': 'text-danger',
    'TRES_ELEVE': 'text-danger fw-bold'
  };

  constructor(private compensationService: CompensationService) {}

  ngOnInit(): void {
    this.loadCompensations();
  }

  loadCompensations(): void {
    this.loading = true;
    this.compensationService.getMyCompensations().subscribe({
      next: (data) => {
        this.compensations = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des compensations: ' + err.message;
        this.loading = false;
      }
    });
  }

  viewCompensationDetails(compensationId: number): void {
    this.compensationService.getMyCompensationDetails(compensationId).subscribe({
      next: (data) => {
        this.selectedCompensation = data;
        this.showDetails = true;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des détails: ' + err.message;
      }
    });
  }

  async payCompensation(compensationId: number): Promise<void> {
    if (confirm('Voulez-vous vraiment payer cette compensation ?')) {
      // Vérifier le solde d'abord
      this.compensationService.checkBalanceBeforePayment(compensationId).subscribe({
        next: (balanceCheck) => {
          if (balanceCheck.sufficientBalance) {
            // Procéder au paiement
            this.compensationService.payCompensation(compensationId).subscribe({
              next: (result) => {
                this.paymentStatus = result;
                this.loadCompensations(); // Recharger la liste
                setTimeout(() => this.paymentStatus = null, 5000);
              },
              error: (err) => {
                this.error = err.error?.error || 'Erreur lors du paiement';
              }
            });
          } else {
            this.error = `Solde insuffisant. Il vous manque ${balanceCheck.difference} DT`;
          }
        },
        error: (err) => {
          this.error = err.error?.error || 'Erreur lors de la vérification du solde';
        }
      });
    }
  }

  closeDetails(): void {
    this.showDetails = false;
    this.selectedCompensation = null;
  }

  getStatusColor(status: string): string {
    return this.statusColors[status] || 'badge bg-secondary';
  }

  getRiskLevelColor(riskLevel: string): string {
    return this.riskLevelColors[riskLevel] || '';
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('fr-FR');
  }

  formatAmount(amount: number): string {
    return amount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' DT';
  }
}