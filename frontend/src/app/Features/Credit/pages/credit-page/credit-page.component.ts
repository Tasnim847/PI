import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { CreditService, Credit, CreditRequest, CreditStatus } from '../../services/credit.service';

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
  imports: [
    CommonModule,
    FormsModule,
    RouterModule
  ],
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

  // Crédit en cours de modification
  editingCredit: Credit | null = null;

  // Exposer les constantes au template
  statusLabels = STATUS_LABELS;
  statusClasses = STATUS_CLASSES;
  creditStatuses = Object.keys(STATUS_LABELS) as CreditStatus[];

  constructor(
    private creditService: CreditService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.checkUserRole();
    // Attendre un peu pour que le localStorage soit rempli
    setTimeout(() => {
      this.loadCredits();
    }, 500);
  }

  // ========== VÉRIFICATION RÔLE ==========
  checkUserRole(): void {
    if (isPlatformBrowser(this.platformId)) {
      try {
        // Récupérer le rôle directement depuis localStorage
        const role = localStorage.getItem('role');
        const firstName = localStorage.getItem('firstName');
        const lastName = localStorage.getItem('lastName');
        const userEmail = localStorage.getItem('userEmail');
        
        console.log('Role from localStorage:', role);
        console.log('User info:', { firstName, lastName, userEmail });
        
        // Vérifier si c'est un ADMIN
        if (role === 'ADMIN') {
          this.isAdmin = true;
        } else {
          this.isAdmin = false;
        }
        
        console.log('isAdmin:', this.isAdmin);
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
        console.error('Erreur complète:', err);
        
        // Gestion spécifique pour "Failed to fetch"
        if (err.status === 0 || err.statusText === 'Unknown Error') {
          this.errorMessage = '❌ Impossible de se connecter au serveur. Vérifiez que le backend est démarré sur le port 8081.';
        } else if (err.status === 403) {
          this.errorMessage = 'Accès non autorisé. Veuillez vous connecter avec les bons identifiants.';
        } else if (err.status === 401) {
          this.errorMessage = 'Session expirée. Veuillez vous reconnecter.';
        } else {
          this.errorMessage = err.error?.message || err.message || 'Erreur lors du chargement des crédits';
        }
        
        this.isLoading = false;
      }
    });
  }

  // ========== DEMANDE DE CRÉDIT (CLIENT) ==========
  submitRequest(): void {
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
        setTimeout(() => this.successMessage = '', 5000);
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

  // ========== ACTIONS ADMIN - APPROUVER ==========
  approveCredit(creditId: number): void {
    if (!this.approveInterestRate || this.approveInterestRate <= 0) {
      this.errorMessage = 'Veuillez entrer un taux d\'intérêt valide';
      return;
    }

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

  // ========== ACTIONS ADMIN - REJETER ==========
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

  // ========== ACTIONS ADMIN - MODIFIER ==========
  editCredit(credit: Credit): void {
    this.editingCredit = { ...credit };
    // Ouvrir le modal Bootstrap
    const modalElement = document.getElementById('editModal');
    if (modalElement) {
      const modal = new (window as any).bootstrap.Modal(modalElement);
      modal.show();
    }
  }

  saveEdit(): void {
    if (!this.editingCredit) return;

    this.creditService.updateCredit(this.editingCredit).subscribe({
      next: () => {
        this.successMessage = `✅ Crédit N°${this.editingCredit!.creditId} modifié avec succès !`;
        this.loadCredits();
        this.editingCredit = null;
        
        // Fermer le modal
        const modalElement = document.getElementById('editModal');
        if (modalElement) {
          const modal = (window as any).bootstrap.Modal.getInstance(modalElement);
          if (modal) modal.hide();
        }
        
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.errorMessage = err.error?.message || 'Erreur lors de la modification';
      }
    });
  }

  // ========== ACTIONS ADMIN - SUPPRIMER ==========
  deleteCredit(creditId: number): void {
    if (confirm(`⚠️ Êtes-vous sûr de vouloir supprimer définitivement le crédit N°${creditId} ?\n\nCette action est irréversible !`)) {
      this.creditService.deleteCredit(creditId).subscribe({
        next: () => {
          this.successMessage = `🗑️ Crédit N°${creditId} supprimé avec succès`;
          this.loadCredits();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          console.error('Erreur:', err);
          this.errorMessage = err.error?.message || 'Erreur lors de la suppression';
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

  isPending(status: CreditStatus): boolean {
    return status === 'PENDING';
  }

  isApprovedOrInRepayment(status: CreditStatus): boolean {
    return status === 'APPROVED' || status === 'IN_REPAYMENT';
  }

  isRejected(status: CreditStatus): boolean {
    return status === 'REJECTED';
  }

  isClosed(status: CreditStatus): boolean {
    return status === 'CLOSED';
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
