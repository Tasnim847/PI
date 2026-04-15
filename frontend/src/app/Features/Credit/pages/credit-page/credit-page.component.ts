import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

// ✅ IMPORT DEPUIS LE SERVICE
import { CreditService, Credit, CreditRequest } from '../../services/credit.service';

// ✅ TYPE pour les statuts (basé sur le service)
type CreditStatus = Credit['status'];

// ========== CONSTANTES POUR LES STATUTS ==========
const STATUS_LABELS: Record<CreditStatus, string> = {
  'PENDING': 'En attente',
  'APPROVED': 'Approuvé',
  'REJECTED': 'Rejeté',
  'IN_REPAYMENT': 'En remboursement',
  'CLOSED': 'Clôturé'
};

const STATUS_CLASSES: Record<CreditStatus, string> = {
  'PENDING': 'status-pending',
  'APPROVED': 'status-approved',
  'REJECTED': 'status-rejected',
  'IN_REPAYMENT': 'status-repayment',
  'CLOSED': 'status-closed'
};

@Component({
  selector: 'app-credit-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './credit-page.component.html',
  styleUrls: ['./credit-page.component.css']
})
export class CreditPageComponent implements OnInit {

  // ========== DONNÉES ==========
  credits: Credit[] = [];
  isAdmin: boolean = false;
  showRequestForm: boolean = false;
  isLoading: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';
  today: string = new Date().toISOString().split('T')[0];

  // Formulaire de demande
  creditRequest: CreditRequest = {
    amount: 0,
    durationInMonths: 24,
    dueDate: ''
  };

  // Taux d'intérêt pour l'approbation
  approveInterestRate: number = 8;

  // Exposer les constantes au template
  statusLabels = STATUS_LABELS;
  statusClasses = STATUS_CLASSES;

  constructor(
    private creditService: CreditService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.checkUserRole();
    this.loadCredits();
  }

  // ========== VÉRIFICATION RÔLE ==========
  checkUserRole(): void {
    if (isPlatformBrowser(this.platformId)) {
      try {
        const userStr = localStorage.getItem('user');
        if (userStr) {
          const user = JSON.parse(userStr);
          this.isAdmin = user.role === 'ADMIN';
        }
      } catch (e) {
        console.error('Erreur lecture localStorage:', e);
        this.isAdmin = false;
      }
    } else {
      this.isAdmin = false;
    }
  }

  // ========== CHARGEMENT DES CRÉDITS ==========
  loadCredits(): void {
    this.isLoading = true;
    this.errorMessage = '';
    
    const observable = this.isAdmin 
      ? this.creditService.getAllCredits() 
      : this.creditService.getMyCredits();
    
    observable.subscribe({
      next: (data) => {
        this.credits = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur:', err);
        
        if (err.status === 403) {
          this.errorMessage = 'Accès non autorisé. Veuillez vous connecter avec un compte ADMIN.';
        } else if (err.status === 401) {
          this.errorMessage = 'Session expirée. Veuillez vous reconnecter.';
        } else {
          this.errorMessage = err.error?.message || 'Erreur lors du chargement des crédits';
        }
        
        this.isLoading = false;
      }
    });
  }

  // ========== DEMANDE DE CRÉDIT ==========
  submitRequest(): void {
    // Validation
    if (!this.creditRequest.amount || this.creditRequest.amount <= 0) {
      this.errorMessage = 'Veuillez entrer un montant valide';
      return;
    }
    if (!this.creditRequest.durationInMonths || this.creditRequest.durationInMonths <= 0) {
      this.errorMessage = 'Veuillez entrer une durée valide';
      return;
    }
    if (!this.creditRequest.dueDate) {
      this.errorMessage = 'Veuillez sélectionner une date d\'échéance';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    
    this.creditService.requestCredit(this.creditRequest).subscribe({
      next: (response) => {
        this.successMessage = `✅ Demande envoyée avec succès ! Référence: CRD-${response.creditId}`;
        this.showRequestForm = false;
        this.loadCredits();
        this.resetForm();
        this.isLoading = false;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.errorMessage = err.error?.error || err.error?.message || 'Erreur lors de la demande';
        this.isLoading = false;
      }
    });
  }

  resetForm(): void {
    this.creditRequest = {
      amount: 0,
      durationInMonths: 24,
      dueDate: ''
    };
  }

  // ========== ACTIONS ADMIN ==========
  approveCredit(creditId: number): void {
    this.creditService.approveCredit(creditId, this.approveInterestRate).subscribe({
      next: () => {
        this.successMessage = `✅ Crédit N°${creditId} approuvé avec succès !`;
        this.loadCredits();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.errorMessage = err.error?.message || 'Erreur lors de l\'approbation';
      }
    });
  }

  rejectCredit(creditId: number): void {
    if (confirm(`Êtes-vous sûr de vouloir rejeter le crédit N°${creditId} ?`)) {
      this.creditService.rejectCredit(creditId).subscribe({
        next: () => {
          this.successMessage = `❌ Crédit N°${creditId} rejeté`;
          this.loadCredits();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          console.error('Erreur:', err);
          this.errorMessage = err.error?.message || 'Erreur lors du rejet';
        }
      });
    }
  }

  // ========== NAVIGATION ==========
  goToPayment(creditId: number): void {
    this.router.navigate(['/public/repayment', creditId]);
  }

  // ========== UTILITAIRES ==========
  getStatusLabel(status: CreditStatus): string {
    return STATUS_LABELS[status] || status;
  }

  getStatusClass(status: CreditStatus): string {
    return STATUS_CLASSES[status] || 'status-default';
  }

  formatDate(date: string | Date): string {
    if (!date) return '-';
    return new Date(date).toLocaleDateString('fr-FR');
  }

  formatAmount(amount: number): string {
    if (!amount && amount !== 0) return '-';
    return amount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' TND';
  }
}