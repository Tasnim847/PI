import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { CreditService, Credit, CreditRequest, CreditStatus } from '../../../services/credit.service';
import { ScoringService, CreditScore } from '../../../services/scoring.service';
import { CreditScoringComponent } from '../scoring/credit-scoring/credit-scoring.component';

// ========== STATUS CONSTANTS ==========
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
    RouterModule,
    CreditScoringComponent
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

  // ========== SCORING DE CRÉDIT ==========
  showScoringModal: boolean = false;
  selectedClientId: number | null = null;
  selectedClientName: string = '';
  creditScores: Map<number, CreditScore> = new Map();
  scoringLoadingMap: Map<number, boolean> = new Map();

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
    private scoringService: ScoringService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.checkUserRole();
    setTimeout(() => {
      this.loadCredits();
    }, 500);
  }

  // ========== ROLE VERIFICATION ==========
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
        console.error('Error reading localStorage:', e);
        this.isAdmin = false;
      }
    } else {
      this.isAdmin = false;
    }
  }

  // ========== LOAD CREDITS ==========
  loadCredits(): void {
    this.isLoading = true;
    this.errorMessage = '';
    
    const observable = this.isAdmin 
      ? this.creditService.getAllCredits() 
      : this.creditService.getMyCredits();
    
    observable.subscribe({
      next: (data) => {
        this.credits = data;
        console.log('Credits loaded:', data);
        this.applyFilters();
        this.isLoading = false;
        
        // If admin, load history for each unique client
        if (this.isAdmin) {
          console.log('Admin detected, loading history');
          this.loadHistoryForAllClients();
        }
      },
      error: (err) => {
        console.error('Complete error:', err);
        
        if (err.status === 0 || err.statusText === 'Unknown Error') {
          this.errorMessage = '❌ Unable to connect to server. Please verify that the backend is running on port 8081.';
        } else if (err.status === 403) {
          this.errorMessage = 'Unauthorized access. Please login with valid credentials.';
        } else if (err.status === 401) {
          this.errorMessage = 'Session expired. Please login again.';
        } else {
          this.errorMessage = err.error?.message || err.message || 'Error loading credits';
        }
        
        this.isLoading = false;
      }
    });
  }

  // ========== LOAD HISTORY FOR ALL CLIENTS ==========
  loadHistoryForAllClients(): void {
    // Get unique client IDs
    const uniqueClientIds = [...new Set(this.credits
      .filter(c => c.clientId)
      .map(c => c.clientId as number))];
    
    console.log('Unique client IDs:', uniqueClientIds);
    
    // Load history for each client
    uniqueClientIds.forEach(clientId => {
      this.loadClientHistory(clientId);
    });
  }

  // ========== LOAD CLIENT HISTORY ==========
  loadClientHistory(clientId: number): void {
    this.historyLoadingMap.set(clientId, true);
    
    console.log(`Loading history for client ${clientId}`);
    
    this.creditService.getClosedCreditsWithAverage(clientId).subscribe({
      next: (data) => {
        console.log(`History received for client ${clientId}:`, data);
        console.log(`Number of closed credits: ${data.credits?.length}`);
        console.log(`Average late percentage: ${data.averageLatePercentage}`);
        this.clientHistoryMap.set(clientId, data);
        this.historyLoadingMap.set(clientId, false);
      },
      error: (err) => {
        console.error(`Error loading history for client ${clientId}:`, err);
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

  // ========== CREDIT REQUEST (CLIENT) ==========
  submitRequest(): void {
    if (!this.creditRequest.amount || this.creditRequest.amount <= 0) {
      this.errorMessage = 'Please enter a valid amount';
      return;
    }
    if (!this.creditRequest.durationInMonths || this.creditRequest.durationInMonths <= 0) {
      this.errorMessage = 'Please enter a valid duration';
      return;
    }
    if (!this.creditRequest.dueDate) {
      this.errorMessage = 'Please select a due date';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    
    this.creditService.requestCredit(this.creditRequest).subscribe({
      next: (response) => {
        this.successMessage = `✅ Request submitted successfully! Reference: CRD-${response.creditId}`;
        this.showRequestForm = false;
        this.loadCredits();
        this.resetForm();
        this.isLoading = false;
        setTimeout(() => this.successMessage = '', 5000);
      },
      error: (err) => {
        console.error('Error:', err);
        this.errorMessage = err.error?.error || err.error?.message || 'Error submitting request';
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

  // ========== ADMIN ACTIONS - APPROVE ==========
  approveCredit(creditId: number): void {
    if (!this.approveInterestRate || this.approveInterestRate <= 0) {
      this.errorMessage = 'Please enter a valid interest rate';
      return;
    }

    this.creditService.approveCredit(creditId, this.approveInterestRate).subscribe({
      next: () => {
        this.successMessage = `✅ Credit #${creditId} approved successfully!`;
        this.loadCredits();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        console.error('Error:', err);
        this.errorMessage = err.error?.message || 'Error approving credit';
      }
    });
  }

  // ========== ADMIN ACTIONS - REJECT ==========
  rejectCredit(creditId: number): void {
    if (confirm(`Are you sure you want to reject credit #${creditId}?`)) {
      this.creditService.rejectCredit(creditId).subscribe({
        next: () => {
          this.successMessage = `❌ Credit #${creditId} rejected`;
          this.loadCredits();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          console.error('Error:', err);
          this.errorMessage = err.error?.message || 'Error rejecting credit';
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
        this.successMessage = `✅ Credit #${this.editingCredit!.creditId} updated successfully!`;
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
        console.error('Error:', err);
        this.errorMessage = err.error?.message || 'Error updating credit';
      }
    });
  }

  // ========== ADMIN ACTIONS - DELETE ==========
  deleteCredit(creditId: number): void {
    if (confirm(`⚠️ Are you sure you want to permanently delete credit #${creditId}?\n\nThis action is irreversible!`)) {
      this.creditService.deleteCredit(creditId).subscribe({
        next: () => {
          this.successMessage = `🗑️ Credit #${creditId} deleted successfully`;
          this.loadCredits();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          console.error('Error:', err);
          this.errorMessage = err.error?.message || 'Error deleting credit';
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

  // ========== OBTENIR LE POURCENTAGE DE RETARD SÉCURISÉ ==========
  getClientAverageLatePercentageSafe(credit: Credit): number {
    if (!this.hasClientId(credit)) return 0;
    const clientId = this.getClientIdForMethods(credit);
    return this.getClientAverageLatePercentage(clientId);
  }

  // ========== OBTENIR LA CLASSE CSS POUR LE POURCENTAGE ==========
  getLatePercentageClass(credit: Credit): string {
    if (!this.hasClientId(credit)) return 'text-success';
    const percentage = this.getClientAverageLatePercentageSafe(credit);
    return percentage > 20 ? 'text-danger' : 'text-success';
  }

  // ========== SCORING DE CRÉDIT ==========
  
  openScoringModal(clientId: number | undefined, clientName: string): void {
    if (!clientId) return;
    this.selectedClientId = clientId;
    this.selectedClientName = clientName;
    this.showScoringModal = true;
  }

  closeScoringModal(): void {
    this.showScoringModal = false;
    this.selectedClientId = null;
    this.selectedClientName = '';
  }

  onScoreCalculated(score: CreditScore): void {
    if (score.clientId) {
      this.creditScores.set(score.clientId, score);
    }
  }

  getClientScore(clientId: number): CreditScore | null {
    return this.creditScores.get(clientId) || null;
  }

  hasScore(clientId: number): boolean {
    return this.creditScores.has(clientId);
  }

  calculateQuickScore(clientId: number | undefined, clientName: string): void {
    if (!clientId || this.scoringLoadingMap.get(clientId)) return;

    this.scoringLoadingMap.set(clientId, true);
    
    this.scoringService.getQuickScore(clientId).subscribe({
      next: (response: any) => {
        const score: CreditScore = {
          clientId: response.clientId,
          clientName: clientName,
          clientEmail: '',
          score: response.score,
          riskLevel: response.riskLevel,
          recommendation: response.recommendation,
          analysis: '',
          calculatedAt: new Date().toISOString(),
          totalCredits: 0,
          activeCredits: 0,
          closedCredits: 0,
          totalAmount: 0,
          currentDebt: 0,
          averageLatePercentage: 0,
          averageMonthlyPayment: 0,
          daysSinceLastCredit: 0
        };
        
        this.creditScores.set(clientId, score);
        this.scoringLoadingMap.set(clientId, false);
      },
      error: (err: any) => {
        console.error('Error calculating quick score:', err);
        this.scoringLoadingMap.set(clientId, false);
      }
    });
  }

  isScoreLoading(clientId: number | undefined): boolean {
    if (!clientId) return false;
    return this.scoringLoadingMap.get(clientId) || false;
  }

  getScoreColor(score: number): string {
    if (score >= 750) return '#28a745'; // Vert
    if (score >= 650) return '#ffc107'; // Jaune
    if (score >= 550) return '#fd7e14'; // Orange
    return '#dc3545'; // Rouge
  }

  getScoreLabel(score: number): string {
    if (score >= 750) return 'Excellent';
    if (score >= 700) return 'Très bon';
    if (score >= 650) return 'Bon';
    if (score >= 600) return 'Acceptable';
    if (score >= 550) return 'Moyen';
    return 'Faible';
  }

  getRiskLevelClass(riskLevel: string): string {
    switch(riskLevel?.toUpperCase()) {
      case 'FAIBLE': return 'bg-success';
      case 'MOYEN': return 'bg-warning';
      case 'ÉLEVÉ': return 'bg-danger';
      case 'TRÈS_ÉLEVÉ': return 'bg-dark';
      default: return 'bg-secondary';
    }
  }

  getRecommendationClass(recommendation: string): string {
    switch(recommendation?.toUpperCase()) {
      case 'APPROUVER': return 'bg-success';
      case 'APPROUVER_AVEC_CONDITIONS': return 'bg-warning';
      case 'REJETER': return 'bg-danger';
      default: return 'bg-secondary';
    }
  }
}