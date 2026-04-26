import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { CreditService, Credit, CreditRequest, CreditStatus } from '../../services/credit.service';

// ========== CONSTANTES POUR LES STATUTS ==========
const STATUS_LABELS: Record<CreditStatus, string> = {
  'PENDING': 'Pending',
  'APPROVED': 'Approved',
  'REJECTED': 'Rejected',
  'IN_REPAYMENT': 'In Repayment',
  'CLOSED': 'Closed'
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

  // Historique des crédits - Map pour stocker l'historique par clientId
  clientHistoryMap: Map<number, any> = new Map();
  historyLoadingMap: Map<number, boolean> = new Map();

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

  // ========== FILTRES ET PAGINATION ==========
  searchTerm: string = '';
  filterStatus: string = '';
  filteredCredits: Credit[] = [];
  
  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;
  totalPages: number = 1;

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
    setTimeout(() => {
      this.loadCredits();
    }, 500);
  }

  // ========== VÉRIFICATION RÔLE ==========
  checkUserRole(): void {
    if (isPlatformBrowser(this.platformId)) {
      try {
        const role = localStorage.getItem('role');
        
        console.log('Role from localStorage:', role);
        
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
        console.log('Crédits chargés:', data);
        this.applyFilters();
        this.isLoading = false;
        
        // Si admin, charger l'historique pour chaque client unique
        if (this.isAdmin) {
          console.log('Admin détecté, chargement de l\'historique');
          this.loadHistoryForAllClients();
        }
      },
      error: (err) => {
        console.error('Erreur complète:', err);
        
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

  // ========== CHARGER L'HISTORIQUE POUR TOUS LES CLIENTS ==========
  loadHistoryForAllClients(): void {
    // Récupérer les IDs clients uniques
    const uniqueClientIds = [...new Set(this.credits
      .filter(c => c.clientId)
      .map(c => c.clientId as number))];
    
    console.log('IDs clients uniques:', uniqueClientIds);
    
    // Charger l'historique pour chaque client
    uniqueClientIds.forEach(clientId => {
      this.loadClientHistory(clientId);
    });
  }

  // ========== CHARGER L'HISTORIQUE D'UN CLIENT ==========
  loadClientHistory(clientId: number): void {
    this.historyLoadingMap.set(clientId, true);
    
    console.log(`Chargement historique pour client ${clientId}`);
    
    this.creditService.getClosedCreditsWithAverage(clientId).subscribe({
      next: (data) => {
        console.log(`Historique reçu pour client ${clientId}:`, data);
        console.log(`Nombre de crédits fermés: ${data.credits?.length}`);
        console.log(`Moyenne de retard: ${data.averageLatePercentage}`);
        this.clientHistoryMap.set(clientId, data);
        this.historyLoadingMap.set(clientId, false);
      },
      error: (err) => {
        console.error(`Erreur chargement historique client ${clientId}:`, err);
        console.error(`Status: ${err.status}`);
        console.error(`Message: ${err.message}`);
        this.historyLoadingMap.set(clientId, false);
      }
    });
  }

  // ========== OBTENIR L'HISTORIQUE D'UN CLIENT ==========
  getClientHistory(clientId: number | undefined): any {
    if (!clientId) return null;
    return this.clientHistoryMap.get(clientId);
  }

  // ========== OBTENIR LA MOYENNE DE RETARD D'UN CLIENT ==========
  getClientAverageLatePercentage(clientId: number | undefined): number {
    if (!clientId) return 0;
    const history = this.clientHistoryMap.get(clientId);
    return history?.averageLatePercentage || 0;
  }

  // ========== VÉRIFIER SI L'HISTORIQUE EST EN COURS DE CHARGEMENT ==========
  isHistoryLoading(clientId: number | undefined): boolean {
    if (!clientId) return false;
    return this.historyLoadingMap.get(clientId) || false;
  }

  // ========== APPLIQUER LES FILTRES ==========
  applyFilters(): void {
    this.filteredCredits = this.credits.filter(credit => {
      const matchesSearch = !this.searchTerm || 
        credit.creditId.toString().includes(this.searchTerm) ||
        credit.clientId?.toString().includes(this.searchTerm) ||
        credit.amount.toString().includes(this.searchTerm);
      
      const matchesStatus = !this.filterStatus || credit.status === this.filterStatus;
      
      return matchesSearch && matchesStatus;
    });
    
    // Recalculer la pagination
    this.totalPages = Math.ceil(this.filteredCredits.length / this.itemsPerPage);
    this.currentPage = 1;
  }

  // ========== OBTENIR LES CRÉDITS PAGINÉS ==========
  getPaginatedCredits(): Credit[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredCredits.slice(startIndex, endIndex);
  }

  // ========== CHANGER DE PAGE ==========
  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  // ========== GÉNÉRER LES NUMÉROS DE PAGE ==========
  getPageNumbers(): number[] {
    const pages: number[] = [];
    for (let i = 1; i <= this.totalPages; i++) {
      pages.push(i);
    }
    return pages;
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

  // ========== MÉTHODES POUR LES INFOS CLIENT ==========
  
  getClientId(credit: Credit): number | string {
    return credit.client?.id || credit.clientId || '-';
  }

  getClientName(credit: Credit): string {
    if (credit.clientFullName) return credit.clientFullName;
    if (credit.client?.firstName) {
      return `${credit.client.firstName} ${credit.client.lastName}`;
    }
    return '-';
  }

  getClientEmail(credit: Credit): string {
    return credit.client?.email || credit.clientEmail || '-';
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

  // ========== CALCUL MONTANT TOTAL ==========
  getTotalAmount(): number {
    return this.filteredCredits.reduce((sum, credit) => sum + credit.amount, 0);
  }

  // ========== OBTENIR LE NOMBRE DE CRÉDITS EN REMBOURSEMENT ==========
  getInRepaymentCount(): number {
    return this.filteredCredits.filter(c => c.status === 'IN_REPAYMENT').length;
  }

  // ========== HELPER POUR VÉRIFIER SI CLIENT ID EXISTE ==========
  hasClientId(credit: Credit): boolean {
    return credit.clientId !== null && credit.clientId !== undefined && credit.clientId > 0;
  }

  // ========== OBTENIR CLIENT ID POUR AFFICHAGE ==========
  getClientIdForDisplay(credit: Credit): number {
    return credit.clientId || 0;
  }

  // ========== OBTENIR CLIENT ID SÉCURISÉ POUR LES MÉTHODES ==========
  getClientIdForMethods(credit: Credit): number | undefined {
    return credit.clientId || undefined;
  }

  // ========== FORMATER LE POURCENTAGE DE RETARD ==========
  formatLatePercentage(credit: Credit): string {
    if (!this.hasClientId(credit)) return '0.00';
    const clientId = this.getClientIdForMethods(credit);
    const percentage = this.getClientAverageLatePercentage(clientId);
    return percentage.toFixed(2);
  }

  // ========== OBTENIR LA CLASSE CSS POUR LE POURCENTAGE ==========
  getLatePercentageClass(credit: Credit): string {
    if (!this.hasClientId(credit)) return 'text-success';
    const clientId = this.getClientIdForMethods(credit);
    const percentage = this.getClientAverageLatePercentage(clientId);
    return percentage > 20 ? 'text-danger' : 'text-success';
  }
}